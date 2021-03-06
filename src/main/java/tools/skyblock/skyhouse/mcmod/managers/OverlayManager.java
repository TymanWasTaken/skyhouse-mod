package tools.skyblock.skyhouse.mcmod.managers;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;
import tools.skyblock.skyhouse.mcmod.gui.*;
import tools.skyblock.skyhouse.mcmod.models.SearchFilter;
import tools.skyblock.skyhouse.mcmod.SkyhouseMod;
import tools.skyblock.skyhouse.mcmod.util.Constants;
import tools.skyblock.skyhouse.mcmod.util.Utils;

import java.io.File;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static tools.skyblock.skyhouse.mcmod.util.Utils.*;

public class OverlayManager {

    private CustomGui gui;
    private JsonArray flips = null;
    private SearchFilter filter;
    private boolean createGui;
    public List<String> auctionBlacklist = new ArrayList<>();
    private boolean creationConfigOpened = false;
    private boolean creationGuiOpened = false;
    private boolean flipListOpened = false;

    private CustomGui ensureInstance() {
        if (renderCreationOverlay() && isAhCreationGui()) {
            if (flipListOpened) {
                if (!(gui instanceof FlipListGui)) gui = new FlipListGui(flips, filter);
                return gui;
            } else if (creationGuiOpened) {
                if (!(gui instanceof CreationGui)) gui = new CreationGui();
                return gui;
            } else if (creationConfigOpened) {
                if (!(gui instanceof CreationConfigGui)) gui = new CreationConfigGui();
                return gui;
            } else {
                gui = new CreationGui();
                this.creationGuiOpened = true;
                return gui;
            }
        } else if (gui == null || (isAhGui() && (gui instanceof CreationGui || gui instanceof CreationConfigGui))) {
            flipListOpened = false;
            gui = new SelectionGui();
        } else if (flips != null && (gui instanceof SelectionGui || createGui)) {
            createGui = false;
            flipListOpened = true;
            gui = new FlipListGui(flips, filter);
        } else if (gui instanceof FlipListGui) {
            return gui;
        }
        if (this.creationGuiOpened) this.creationGuiOpened = false;
        if (this.creationConfigOpened) this.creationConfigOpened = false;
        if (this.flipListOpened) this.creationConfigOpened = false;
        return gui;
    }

    public void close() {
        gui = null;
        flips = null;
        flipListOpened = false;
    }

    public void keyTyped() {
        ensureInstance().keyEvent();
    }

    public void mouseAction() {
        ensureInstance();
        if (!Mouse.getEventButtonState()) return;
        int mouseX = Mouse.getX() * gui.width / Minecraft.getMinecraft().displayWidth;
        int mouseY = gui.height - Mouse.getY() * gui.height / Minecraft.getMinecraft().displayHeight - 1;
        ensureInstance().click(mouseX, mouseY);
    }

    public void drawScreen(int mouseX, int mouseY) {
        ensureInstance().tick();
        gui.drawScreen(mouseX, mouseY);
    }

    public void initGui() {
        ensureInstance().tick();
        gui.initGui();
    }

    public void search(SearchFilter filter) {
        this.filter = filter;
        Utils.getJsonApiAsync(Utils.getUrl("https://api-jiri-v1-91372cec-b8ed-4f23-9572-f2c3219cf6f8.rose.sh/api/flip/auctions",//"https://api.skyblock.tools/skyhouse/api/flip/auctions",
                SkyhouseMod.gson.fromJson(SkyhouseMod.serializeGson.toJson(filter), JsonObject.class)),
                data -> {
                    flips = data.get("flips").getAsJsonArray();
                    createGui = true;
                }, e -> {
                    if (e instanceof SocketTimeoutException) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could not connect to API!"));
                        Minecraft.getMinecraft().displayGuiScreen(null);
                    } else if (e.getMessage().contains("403") || e.getMessage().contains("401")) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Forbidden to access the API!"));
                        Minecraft.getMinecraft().displayGuiScreen(null);
                    } else {
                        CrashReport report = CrashReport.makeCrashReport(e, "API returned unknown error");
                        File crashDir = new File(SkyhouseMod.INSTANCE.getConfigDir(), "errors");
                        crashDir.mkdirs();
                        File reportFile = new File(crashDir, "error-" + (new SimpleDateFormat("yyyy-dd-MM_HH.mm.ss")).format(new Date()) + ".txt");
                        report.saveToFile(reportFile);
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "An unknown exception occured. Log saved to file"));
                        Minecraft.getMinecraft().displayGuiScreen(null);
                        e.printStackTrace();
                    }
                });
    }

    public void toggleFlipListCreationGui() {
        this.flipListOpened = !this.flipListOpened;
        if (this.creationGuiOpened) this.creationGuiOpened = false;
        else if (this.creationConfigOpened) this.creationConfigOpened = false;
    }

    public void toggleCreationConfig() {
        if (this.creationConfigOpened) CreationConfigGui.onGuiClose();
        this.creationConfigOpened = !this.creationConfigOpened;
        this.creationGuiOpened = !this.creationGuiOpened;
    }

    public boolean hasFlips() {
        return flips != null;
    }

    public void drawHoveringText(List<String> text, int x, int y) {
        ensureInstance().drawHoveringText(text, x, y);
    }

    public void resetFilter() {
        SkyhouseMod.INSTANCE.getConfigManager().setMaxPrice(Constants.DEFAULT_MAX_PRICE);
        SkyhouseMod.INSTANCE.getConfigManager().setMinProfit(Constants.DEFAULT_MIN_PROFIT);
        SkyhouseMod.INSTANCE.getConfigManager().setSkinsInSearch(true);
        SkyhouseMod.INSTANCE.getConfigManager().setCakeSoulsInSearch(true);
        SkyhouseMod.INSTANCE.getConfigManager().setPetsInSearch(true);
        SkyhouseMod.INSTANCE.getConfigManager().setRecombsInSearch(true);
        gui = new SelectionGui();
    }

    public boolean isFilterDefault() {
        return SkyhouseMod.INSTANCE.getConfigManager().maxPrice == Constants.DEFAULT_MAX_PRICE &&
                SkyhouseMod.INSTANCE.getConfigManager().minProfit == Constants.DEFAULT_MIN_PROFIT &&
                SkyhouseMod.INSTANCE.getConfigManager().skinsInSearch == Constants.DEFAULT_SKINS_IN_SEARCH &&
                SkyhouseMod.INSTANCE.getConfigManager().cakeSoulsInSearch == Constants.DEFAULT_CAKE_SOULS_IN_SEARCH &&
                SkyhouseMod.INSTANCE.getConfigManager().petsInSearch == Constants.DEFAULT_PETS_IN_SEARCH &&
                SkyhouseMod.INSTANCE.getConfigManager().recombsInSearch == Constants.DEFAULT_RECOMBS_IN_SEARCH;
    }

}
