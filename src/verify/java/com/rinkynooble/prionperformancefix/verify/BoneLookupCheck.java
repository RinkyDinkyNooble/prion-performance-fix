package com.rinkynooble.prionperformancefix.verify;

import com.rinkynooble.prionperformancefix.client.AllParts;
import com.rinkynooble.prionperformancefix.client.BoneIndex;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

/**
 * Dev-only check, run with {@code gradlew verifyBones}. Never shipped.
 *
 * <p>Runs in a plain JVM, so no mixin is applied and {@code HierarchicalModel.getAnyDescendantWithName}
 * and {@code ModelPart.getAllParts} are Minecraft's own code. Compares them with {@link BoneIndex}
 * and {@link AllParts} on every Prion model (with every part as the root, for every child name and
 * every animated bone name), on generated part trees with repeated names and different
 * children-map types, and by playing every Prion animation on two copies of every Prion model, one
 * per lookup, and comparing every part's pose. Prints PASS or FAIL.
 */
public final class BoneLookupCheck {
    private static final Field CHILDREN;

    static {
        try {
            CHILDREN = ModelPart.class.getDeclaredField("children");
            CHILDREN.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int compared;
    private static int unsupported;
    private static int partLists;
    private static final List<String> FAILURES = new ArrayList<>();

    public static void main(String[] args) throws Throwable {
        Map<String, LayerDefinition> layers = prionLayers();
        Map<String, AnimationDefinition> animations = prionAnimations();
        Set<String> boneNames = new LinkedHashSet<>();
        animations.values().forEach(definition -> boneNames.addAll(definition.boneAnimations().keySet()));
        System.out.printf("Prion models: %d, animations: %d, animated bone names: %d%n", layers.size(), animations.size(), boneNames.size());

        for (Map.Entry<String, LayerDefinition> layer : layers.entrySet()) {
            ModelPart root = layer.getValue().bakeRoot();
            for (ModelPart part : root.getAllParts().toList()) {
                compareLookups(layer.getKey(), part, boneNames);
                compareParts(layer.getKey(), part);
            }
        }
        int prionCompared = compared;
        int prionPartLists = partLists;
        System.out.printf("Prion lookups compared: %d, part lists: %d, trees left to Minecraft: %d%n", prionCompared, prionPartLists, unsupported);

        Random random = new Random(20261012L);
        for (int i = 0; i < 20000; i++) {
            ModelPart root = randomTree(random, i % MAP_TYPES.size(), 0);
            for (ModelPart part : root.getAllParts().toList()) {
                compareLookups("generated #" + i, part, Set.of("a", "b", "c", "d", "e", "f", "missing"));
                compareParts("generated #" + i, part);
            }
        }
        System.out.printf("Generated-tree lookups compared: %d, part lists: %d%n", compared - prionCompared, partLists - prionPartLists);

        int poses = comparePoses(layers, animations);
        System.out.printf("Animation poses compared: %d part poses%n", poses);

        timeRooted(layers.get("RootedModel"), animations);
        timeResetPose(layers.get("ViciousturretModel"));

        if (FAILURES.isEmpty()) {
            System.out.println("PASS");
        } else {
            FAILURES.stream().limit(40).forEach(System.out::println);
            System.out.println("FAIL: " + FAILURES.size() + " mismatches");
            System.exit(1);
        }
    }

    /** Minecraft's lookup against the index, for every child name under {@code root} plus {@code extraNames}. */
    private static void compareLookups(String what, ModelPart root, Set<String> extraNames) {
        Map<String, Optional<ModelPart>> index = BoneIndex.build(root, BoneLookupCheck::children);
        if (index == null) {
            unsupported++;
            return;
        }
        HierarchicalModel<Entity> vanilla = modelOf(root);
        Set<String> names = new LinkedHashSet<>(extraNames);
        root.getAllParts().forEach(part -> names.addAll(children(part).keySet()));
        names.remove("root");
        for (String name : names) {
            Optional<ModelPart> expected = vanilla.getAnyDescendantWithName(name);
            Optional<ModelPart> actual = index.getOrDefault(name, Optional.empty());
            compared++;
            if (expected.isPresent() != actual.isPresent() || expected.isPresent() && expected.get() != actual.get()) {
                FAILURES.add("MISMATCH " + what + " name=" + name);
            }
        }
    }

    /** Minecraft's {@code getAllParts} against the list: the same parts in the same order. */
    private static void compareParts(String what, ModelPart part) {
        List<ModelPart> list = AllParts.build(part, BoneLookupCheck::children);
        if (list == null) {
            return;
        }
        List<ModelPart> expected = part.getAllParts().toList();
        partLists++;
        boolean same = expected.size() == list.size();
        for (int i = 0; same && i < expected.size(); i++) {
            same = expected.get(i) == list.get(i);
        }
        if (!same) {
            FAILURES.add("PART LIST " + what);
        }
    }

    /** Time per frame for the pose reset every Prion model does first: {@code root().getAllParts().forEach(ModelPart::resetPose)}. */
    private static void timeResetPose(LayerDefinition layer) {
        ModelPart root = layer.bakeRoot();
        List<ModelPart> list = AllParts.build(root, BoneLookupCheck::children);
        long vanillaNanos = 0;
        long listNanos = 0;
        for (int round = 0; round < 6; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < 2000; i++) {
                root.getAllParts().forEach(ModelPart::resetPose);
            }
            long middle = System.nanoTime();
            for (int i = 0; i < 2000; i++) {
                list.stream().forEach(ModelPart::resetPose);
            }
            long end = System.nanoTime();
            if (round >= 2) {
                vanillaNanos += middle - start;
                listNanos += end - middle;
            }
        }
        long frames = 4L * 2000;
        System.out.printf("Vicious Turret pose reset, %d parts: Minecraft %.1f us, list %.2f us per frame%n",
                list.size(), vanillaNanos / 1000.0 / frames, listNanos / 1000.0 / frames);
    }

    /** Plays every animation on two copies of every model, one per lookup, and compares the poses. */
    private static int comparePoses(Map<String, LayerDefinition> layers, Map<String, AnimationDefinition> animations) {
        long[] times = {0, 137, 500, 1234, 3333, 7777};
        float[] scales = {1.0F, 0.5F};
        int poses = 0;
        for (Map.Entry<String, LayerDefinition> layer : layers.entrySet()) {
            ModelPart vanillaRoot = layer.getValue().bakeRoot();
            ModelPart indexedRoot = layer.getValue().bakeRoot();
            HierarchicalModel<Entity> vanilla = modelOf(vanillaRoot);
            HierarchicalModel<Entity> indexed = indexedModelOf(indexedRoot);
            List<ModelPart> vanillaParts = vanillaRoot.getAllParts().toList();
            List<ModelPart> indexedParts = indexedRoot.getAllParts().toList();
            for (Map.Entry<String, AnimationDefinition> animation : animations.entrySet()) {
                for (long time : times) {
                    for (float scale : scales) {
                        vanillaParts.forEach(ModelPart::resetPose);
                        indexedParts.forEach(ModelPart::resetPose);
                        KeyframeAnimations.animate(vanilla, animation.getValue(), time, scale, new Vector3f());
                        KeyframeAnimations.animate(indexed, animation.getValue(), time, scale, new Vector3f());
                        for (int i = 0; i < vanillaParts.size(); i++) {
                            poses++;
                            if (!samePose(vanillaParts.get(i), indexedParts.get(i))) {
                                FAILURES.add("POSE " + layer.getKey() + " " + animation.getKey() + " t=" + time + " part #" + i);
                            }
                        }
                    }
                }
            }
        }
        return poses;
    }

    /** Lookup time per animated bone for the Rooted, the worst mob in the profile. */
    private static void timeRooted(LayerDefinition layer, Map<String, AnimationDefinition> animations) {
        ModelPart root = layer.bakeRoot().getChild("rooted");
        HierarchicalModel<Entity> vanilla = modelOf(root);
        Map<String, Optional<ModelPart>> index = BoneIndex.build(root, BoneLookupCheck::children);
        List<String> bones = new ArrayList<>();
        for (String name : List.of("ROOTEDMOVING", "ROOTEDIDLE", "DIEANIM")) {
            AnimationDefinition definition = animations.get(name);
            if (definition != null) {
                bones.addAll(definition.boneAnimations().keySet());
            }
        }
        long sink = 0;
        long vanillaNanos = 0;
        long indexNanos = 0;
        for (int round = 0; round < 6; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < 2000; i++) {
                for (String bone : bones) {
                    sink += vanilla.getAnyDescendantWithName(bone).isPresent() ? 1 : 0;
                }
            }
            long middle = System.nanoTime();
            for (int i = 0; i < 2000; i++) {
                for (String bone : bones) {
                    Optional<ModelPart> found = index.get(bone);
                    sink += found != null && found.isPresent() ? 1 : 0;
                }
            }
            long end = System.nanoTime();
            if (round >= 2) {
                vanillaNanos += middle - start;
                indexNanos += end - middle;
            }
        }
        long frames = 4L * 2000;
        System.out.printf("Rooted, %d bone lookups per frame: Minecraft %.1f us, index %.2f us per frame (%d)%n",
                bones.size(), vanillaNanos / 1000.0 / frames, indexNanos / 1000.0 / frames, sink % 10);
    }

    private static boolean samePose(ModelPart a, ModelPart b) {
        return a.x == b.x && a.y == b.y && a.z == b.z && a.xRot == b.xRot && a.yRot == b.yRot && a.zRot == b.zRot
                && a.xScale == b.xScale && a.yScale == b.yScale && a.zScale == b.zScale && a.visible == b.visible;
    }

    private static HierarchicalModel<Entity> modelOf(ModelPart root) {
        return new HierarchicalModel<>(texture -> null) {
            @Override
            public ModelPart root() {
                return root;
            }

            @Override
            public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            }
        };
    }

    /** A model whose lookup is the index, as the mixin makes it in game. */
    private static HierarchicalModel<Entity> indexedModelOf(ModelPart root) {
        Map<String, Optional<ModelPart>> index = BoneIndex.build(root, BoneLookupCheck::children);
        if (index == null) {
            throw new IllegalStateException("A Prion model isn't supported by the index");
        }
        return new HierarchicalModel<>(texture -> null) {
            @Override
            public ModelPart root() {
                return root;
            }

            @Override
            public Optional<ModelPart> getAnyDescendantWithName(String name) {
                return name.equals("root") ? Optional.of(root) : index.getOrDefault(name, Optional.empty());
            }

            @Override
            public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ModelPart> children(ModelPart part) {
        try {
            return (Map<String, ModelPart>) CHILDREN.get(part);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final List<Supplier<Map<String, ModelPart>>> MAP_TYPES = List.of(
            HashMap::new, LinkedHashMap::new, TreeMap::new, Object2ObjectArrayMap::new, Object2ObjectOpenHashMap::new);

    /** A small tree whose names repeat, so the first match in search order matters. */
    private static ModelPart randomTree(Random random, int mapType, int depth) {
        Map<String, ModelPart> children = MAP_TYPES.get(mapType).get();
        int count = depth >= 6 ? 0 : random.nextInt(depth == 0 ? 5 : 4);
        String[] pool = {"a", "b", "c", "d", "e", "f", "root"};
        for (int i = 0; i < count; i++) {
            children.put(pool[random.nextInt(pool.length)], randomTree(random, random.nextInt(MAP_TYPES.size()), depth + 1));
        }
        return new ModelPart(List.of(), children);
    }

    /**
     * Every Prion model layer, by model class name, from its static {@code getTexturedModelData}.
     * Looked up as a single method so the models' entity classes (which need Fabric API) never load.
     */
    private static Map<String, LayerDefinition> prionLayers() throws Throwable {
        Map<String, LayerDefinition> layers = new TreeMap<>();
        for (Class<?> type : prionClasses("net/demod/prionmod/entity/client/")) {
            MethodHandle factory = MethodHandles.lookup().findStatic(type, "getTexturedModelData", MethodType.methodType(LayerDefinition.class));
            layers.put(type.getSimpleName(), (LayerDefinition) factory.invoke());
        }
        return layers;
    }

    /** Every Prion animation, by field name. */
    private static Map<String, AnimationDefinition> prionAnimations() throws Exception {
        Map<String, AnimationDefinition> animations = new TreeMap<>();
        for (Class<?> type : prionClasses("net/demod/prionmod/entity/animation/")) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == AnimationDefinition.class) {
                    field.setAccessible(true);
                    animations.put(field.getName(), (AnimationDefinition) field.get(null));
                }
            }
        }
        return animations;
    }

    private static List<Class<?>> prionClasses(String packagePath) throws Exception {
        File jar = new File(KeyframeAnimations.class.getClassLoader()
                .getResource("net/demod/prionmod/entity/animation/ModAnimations.class").toURI().toString()
                .replaceFirst("^jar:file:", "").replaceFirst("!.*$", ""));
        List<Class<?>> classes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(java.net.URLDecoder.decode(jar.getPath(), java.nio.charset.StandardCharsets.UTF_8))) {
            Function<String, String> toName = entry -> entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
            for (String entry : zip.stream().map(java.util.zip.ZipEntry::getName).toList()) {
                if (entry.startsWith(packagePath) && entry.endsWith(".class") && !entry.substring(packagePath.length()).contains("/")
                        && (packagePath.endsWith("animation/") || entry.endsWith("Model.class"))) {
                    classes.add(Class.forName(toName.apply(entry), false, BoneLookupCheck.class.getClassLoader()));
                }
            }
        }
        return classes;
    }
}
