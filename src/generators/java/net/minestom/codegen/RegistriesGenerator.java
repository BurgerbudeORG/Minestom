package net.minestom.codegen;

import net.minestom.server.entity.EntityType;
import net.minestom.server.fluids.Fluid;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.Material;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.PotionType;
import net.minestom.server.registry.ResourceGatherer;
import net.minestom.server.sound.Sound;
import net.minestom.server.stat.StatisticType;
import net.minestom.server.utils.NamespaceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

import static net.minestom.codegen.MinestomEnumGenerator.DEFAULT_TARGET_PATH;

/**
 * Generates the Registries class, which contains methods to get items/blocks/biomes/etc. from a NamespaceID
 */
public class RegistriesGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistriesGenerator.class);

    // Order is important!
    private static final String[] types = {
            Block.class.getCanonicalName(),
            Material.class.getCanonicalName(),
            Enchantment.class.getCanonicalName(),
            EntityType.class.getCanonicalName(),
            Particle.class.getCanonicalName(),
            PotionType.class.getCanonicalName(),
            PotionEffect.class.getCanonicalName(),
            Sound.class.getCanonicalName(),
            StatisticType.class.getCanonicalName(),
            Fluid.class.getCanonicalName(),
    };
    private static final String[] defaults = {
            "AIR",
            "AIR",
            null,
            "PIG",
            null,
            null,
            null,
            null,
            null,
            "EMPTY"
    };

    @Override
    public String generate() throws IOException {
        StringBuilder contents = new StringBuilder(
                "package net.minestom.server.registry;\n\n" +
                        "import " + HashMap.class.getCanonicalName() + ";\n" +
                        "import " + NamespaceID.class.getCanonicalName() + ";\n");

        for (String type : types) {
            contents.append("import ").append(type).append(";\n");
        }

        contents.append("\n// AUTOGENERATED\npublic class Registries {\n");

        if (types.length != defaults.length) {
            throw new Error("types.length != defaults.length");
        }

        // Hashmaps
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            String simpleType = type.substring(type.lastIndexOf('.') + 1);
            contents.append("\t/** Should only be used for internal code, please use the get* methods. */\n");
            contents.append("\t@Deprecated\n");
            contents.append('\t');
            contents.append("public static final HashMap<NamespaceID, ").append(simpleType).append("> ").append(CodeGenerator.decapitalize(simpleType)).append('s');
            contents.append(" = new HashMap<>();").append("\n\n");
        }

        contents.append('\n');

        // accessor methods
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            String simpleType = type.substring(type.lastIndexOf('.') + 1);
            String defaultValue = defaults[i];

            // Example:
/*
            /** Returns 'AIR' if none match
            public static Block getBlock(String id) {
                return getBlock(NamespaceID.from(id));
            }

            /** Returns 'AIR' if none match
            public static Block getBlock(NamespaceID id) {
                return blocks.getOrDefault(id, AIR);
            }
*/
            StringBuilder comment = new StringBuilder("/** Returns the corresponding ");
            comment.append(simpleType).append(" matching the given id. Returns ");
            if (defaultValue != null) {
                comment.append('\'').append(defaultValue).append('\'');
            } else {
                comment.append("null");
            }
            comment.append(" if none match. */");

            // String variant
            contents.append('\t').append(comment).append("\n");
            contents.append('\t');
            contents.append("public static ").append(simpleType).append(" get").append(simpleType).append("(String id) {\n");
            contents.append("\t\t").append("return get").append(simpleType).append("(NamespaceID.from(id));\n");

            contents.append("\t}\n\n");

            // NamespaceID variant
            contents.append('\t').append(comment).append("\n");
            contents.append('\t');
            contents.append("public static ").append(simpleType).append(" get").append(simpleType).append("(NamespaceID id) {\n");
            contents.append("\t\t").append("return ").append(CodeGenerator.decapitalize(simpleType)).append("s.");

            if (defaultValue != null) {
                contents.append("getOrDefault(id, ").append(simpleType).append('.').append(defaultValue).append(");");
            } else {
                contents.append("get(id);");
            }

            contents.append("\n");

            contents.append("\t}\n\n");
        }

        contents.append("\n}\n");

        return contents.toString();
    }

    public static void main(String[] args) throws IOException {
        // copy-pasted from BlockEnumGenerator, to stay consistent in the order of arguments
        String targetVersion;
        if (args.length < 1) {
            System.err.println("Usage: <MC version> [target folder]");
            return;
        }

        targetVersion = args[0];

        try {
            ResourceGatherer.ensureResourcesArePresent(targetVersion); // TODO
        } catch (IOException e) {
            e.printStackTrace();
        }

        String targetPart = DEFAULT_TARGET_PATH;
        if (args.length >= 2) {
            targetPart = args[1];
        }

        File targetFolder = new File(targetPart);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        new RegistriesGenerator().generateTo(targetFolder);
    }

    private void generateTo(File targetFolder) throws IOException {
        String code = generate();
        String folder = "net/minestom/server/registry";
        File parentFolder = new File(targetFolder, folder);
        if (!parentFolder.exists()) {
            parentFolder.mkdirs();
        }

        LOGGER.debug("Writing to file: " + parentFolder + "/Registries.java");
        try (Writer writer = new BufferedWriter(new FileWriter(new File(parentFolder, "Registries.java")))) {
            writer.write(code);
        }
    }
}
