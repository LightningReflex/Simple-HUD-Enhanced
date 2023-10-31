package com.soradgaming.simplehudenhanced.hud;

import com.soradgaming.simplehudenhanced.config.SimpleHudEnhancedConfig;
import com.soradgaming.simplehudenhanced.utli.Colours;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class HUD {
    // Minecraft client variables
    private final MinecraftClient client;
    private final TextRenderer renderer;

    //Config
    private SimpleHudEnhancedConfig config;

    public HUD(MinecraftClient client) {
        this.client = client;

        this.renderer = client.textRenderer;

        this.config = AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).getConfig();

        AutoConfig.getConfigHolder(SimpleHudEnhancedConfig.class).registerSaveListener((manager, data) -> {
            // Update local config when new settings are saved
            this.config = data;
            return ActionResult.SUCCESS;
        });
    }

    // Main HUD function to draw all the elements on the screen
    public void drawAsyncHud(MatrixStack matrixStack) {
        // Check if HUD is enabled
        if (!config.uiConfig.toggleSimpleHUDEnhanced) return;

        // Instance of Class with all the Game Information
        GameInfo GameInformation = new GameInfo(this.client);

        // Draw HUD
        CompletableFuture<Void> statusElementsFuture = CompletableFuture.runAsync(() -> drawStatusElements(matrixStack, GameInformation), MinecraftClient.getInstance()::executeTask);

        // Draw Equipment Status
        CompletableFuture<Void> equipmentFuture = CompletableFuture.runAsync(() -> {
            if (config.toggleEquipmentStatus) {
                Equipment equipment = new Equipment(matrixStack, config);
                equipment.init();
            }
        }, MinecraftClient.getInstance()::executeTask);

        // Draw Movement Status
        if (config.toggleMovementStatus) {
            Movement movement = new Movement(matrixStack, config);
            movement.init(GameInformation);
        }

        // Draw Time
        drawTime(matrixStack, GameInformation.getSystemTime());

        // Ensure completion of all tasks before moving forward
        CompletableFuture.allOf(equipmentFuture, statusElementsFuture).join();
    }

    @NotNull
    private static ArrayList<String> getHudInfo(GameInfo GameInformation) {
        ArrayList<String> hudInfo = new ArrayList<>();

        // Add all the lines to the array
        hudInfo.add(GameInformation.getCords() + GameInformation.getDirection() + GameInformation.getOffset());
        hudInfo.add(GameInformation.getNether());
        hudInfo.add(GameInformation.getFPS());
        hudInfo.add(GameInformation.getSpeed());
        hudInfo.add(GameInformation.getLightLevel());
        hudInfo.add(GameInformation.getBiome());
        hudInfo.add(GameInformation.getTime());
        hudInfo.add(GameInformation.getPlayerName());
        hudInfo.add(GameInformation.getPing());
        hudInfo.add(GameInformation.getServer());
        hudInfo.add(GameInformation.getServerAddress());
        return hudInfo;
    }

    public int getColor(String line, GameInfo GameInformation) {
        int colour = config.uiConfig.textColor;

        // FPS Colour Check
        if (Objects.equals(line, GameInformation.getFPS())) {
            if (config.statusElements.fps.toggleColourFPS) {
                // convert line to int format (102 fps)
                String[] fps = line.split(" ");
                int fpsInt = Integer.parseInt(fps[0]);

                // Check FPS and return colour
                if (fpsInt < 15) {
                    return Colours.RED;
                } else if (fpsInt < 30) {
                    return Colours.lightRed;
                } else if (fpsInt < 45) {
                    return Colours.lightOrange;
                } else if (fpsInt < 60) {
                    return Colours.lightYellow;
                } else {
                    return Colours.GREEN;
                }
            }
        }

        return colour;
    }

    private void drawStatusElements(MatrixStack matrixStack, GameInfo gameInformation) {
        // Get all the lines to be displayed
        ArrayList<String> hudInfo = getHudInfo(gameInformation);

        //Remove empty lines from the array
        hudInfo.removeIf(String::isEmpty);

        // Draw HUD
        int Xcords = config.statusElements.Xcords;
        int Ycords = config.statusElements.Ycords;
        float Scale = (float) config.uiConfig.textScale / 100;

        // Get the longest string in the array
        int longestString = 0;
        int BoxWidth = 0;
        for (String s : hudInfo) {
            if (s.length() > longestString) {
                longestString = s.length();
                BoxWidth = this.renderer.getWidth(s);
            }
        }

        int lineHeight = (this.renderer.fontHeight);

        // Screen Manager
        ScreenManager screenManager = new ScreenManager(this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
        screenManager.setPadding(4);
        int xAxis = screenManager.calculateXAxis(Xcords, Scale, BoxWidth);
        int yAxis = screenManager.calculateYAxis(lineHeight, hudInfo.size(), Ycords, Scale);
        screenManager.setScale(matrixStack, Scale);

        for (String line : hudInfo) {
            int offset = 0;
            if (Xcords >= 50) {
                int lineLength = this.renderer.getWidth(line);
                offset = (BoxWidth - lineLength);
            }
            // Colour Check
            int colour = getColor(line, gameInformation);
            // Render the line
            this.renderer.drawWithShadow(matrixStack, line, xAxis + offset, yAxis, colour);
            yAxis += lineHeight;
        }

        screenManager.resetScale(matrixStack);
    }

    private void drawTime(MatrixStack matrixStack, String systemTime) {
        // Screen Manager
        ScreenManager timeScreenManager = new ScreenManager(this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
        timeScreenManager.setPadding(2);
        float timeScale = (float) config.statusElements.systemTime.textScale / 100;
        int xAxisTime = timeScreenManager.calculateXAxis(100, timeScale, this.renderer.getWidth(systemTime));
        int yAxisTime = timeScreenManager.calculateYAxis(this.renderer.fontHeight, 1, 100, timeScale);
        timeScreenManager.setScale(matrixStack, timeScale);

        // Draw System Time on Bottom Right of Screen
        this.renderer.drawWithShadow(matrixStack, systemTime, xAxisTime, yAxisTime, config.uiConfig.textColor);

        timeScreenManager.resetScale(matrixStack);
    }
}
