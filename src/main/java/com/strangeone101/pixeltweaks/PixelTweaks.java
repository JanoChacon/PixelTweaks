package com.strangeone101.pixeltweaks;

import com.strangeone101.pixeltweaks.listener.ClientListener;
import com.strangeone101.pixeltweaks.listener.CommonListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PixelTweaks.MODID)
public class PixelTweaks {

    public static final String MODID = "pixeltweaks";

    public static final int SHINY_COLOR = 0xe8aa00;

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public PixelTweaks() {

        //Make the server tell clients it is fine to join without it
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new ClientListener();
        }

        new CommonListener();
    }

    public static InputStream getResource(String filename) throws IOException {
        URL url = PixelTweaks.class.getClassLoader().getResource("assets/" + MODID + "/" + filename);

        if (url == null) {
            return null;
        }

        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }
}
