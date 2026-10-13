package dev.skyjet;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkyJet {
    public static final String MOD_ID = "skyjet";
    public static final String MOD_NAME = "SkyJet";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private SkyJet() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
