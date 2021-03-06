package tools.skyblock.skyhouse.mcmod.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.io.IOUtils;
import tools.skyblock.skyhouse.mcmod.SkyhouseMod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Utils {

    public static final ExecutorService es = Executors.newFixedThreadPool(3);
    private static final DecimalFormat df = new DecimalFormat("###,###,###");
    private static final List<String> bonzoFraggedItems = Arrays.asList("BONZO_STAFF", "STARRED_BONZO_STAFF", "BONZO_MASK", "STARRED_BONZO_MASK");
    private static final List<String> scarfFraggedItems = Arrays.asList("STONE_BLADE", "STARRED_STONE_BLADE", "ADAPTIVE_HELMET", "STARRED_ADAPTIVE_HELMET", "ADAPTIVE_CHESTPLATE",
                                                        "STARRED_ADAPTIVE_CHESTPLATE", "ADAPTIVE_LEGGINGS", "STARRED_ADAPTIVE_LEGGINGS", "ADAPTIVE_BOOTS", "STARRED_ADAPTIVE_BOOTS");
    private static final List<String> lividFraggedItems = Arrays.asList("LAST_BREATH", "STARRED_LAST_BREATH", "SHADOW_ASSASSIN_HELMET", "STARRED_SHADOW_ASSASSIN_HELMET",
                                                        "SHADOW_ASSASSIN_CHESTPLATE", "STARRED_SHADOW_ASSASSIN_CHESTPLATE", "SHADOW_ASSASSIN_LEGGINGS", "STARRED_SHADOW_ASSASSIN_LEGGINGS",
                                                        "SHADOW_ASSASSIN_BOOTS", "STARRED_SHADOW_ASSASSIN_BOOTS", "SHADOW_FURY", "STARRED_SHADOW_FURY");


    public static Integer parseInt(String string, int def) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    public static boolean isAhGui() {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            String title = ((ContainerChest) chest.inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();
            return title.toLowerCase().contains("auction") || title.toLowerCase().contains("bid");
        }
        return false;
    }

    public static boolean isAhCreationGui() {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiChest) {
            GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
            String title = ((ContainerChest) chest.inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();
            return title.toLowerCase().contains("create") && title.toLowerCase().contains("auction");
        }
        return false;
    }

    public static URL getUrl(String url, JsonObject query) {
        StringBuilder bobTheBuilder = new StringBuilder(url);
        bobTheBuilder.append("?");
        for (Map.Entry<String, JsonElement> item : query.entrySet()) {
            bobTheBuilder.append(item.getKey())
                    .append("=")
                    .append(item.getValue())
                    .append("&");
        }
        try {
            return new URL(bobTheBuilder.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static JsonObject getJsonApi(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(3_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("accept", "application/json");
        conn.setRequestProperty("user-agent", "forge/skyhouse");
        String res = IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
        return SkyhouseMod.gson.fromJson(res, JsonObject.class);
    }

    public static void getJsonApiAsync(URL url, Consumer<JsonObject> cb) {
        getJsonApiAsync(url, cb, Throwable::printStackTrace);
    }

    public static void getJsonApiAsync(URL url, Consumer<JsonObject> cb, Consumer<IOException> errorHandler) {
        es.submit(() -> {
            try {
                cb.accept(getJsonApi(url));
            } catch (IOException e) {
                errorHandler.accept(e);
            }
        });
    }

    public static void getLowestBinsFromMoulberryApi() {
        getJsonApiAsync(Utils.getUrl("https://moulberry.codes/lowestbin.json", new JsonObject()),
                data -> {
                    if (data != null) SkyhouseMod.INSTANCE.lowestBins = data;
                },
                e -> {
                    System.out.println("Error connecting to Moulberry's lowest bins api");
                    if (SkyhouseMod.INSTANCE.getListener().binsManuallyRefreshed)  {
                        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.RED + "Error connecting to Moulberry's lowest bins api"));
                    }
                });
    }

    public static void getBazaarDataFromApi() {
        getJsonApiAsync(Utils.getUrl("https://api.hypixel.net/skyblock/bazaar", new JsonObject()),
                data -> {
                    if (data != null) SkyhouseMod.INSTANCE.bazaarData = data;
                },
                e -> {
                    System.out.println("Error connecting to Hypixel api");
                    if (SkyhouseMod.INSTANCE.getListener().bazaarManuallyRefreshed)  {
                        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.RED + "Error connecting to Hypixel api"));
                    }
                });
    }

    public static void getReforgeDataFromMoulberryGithub() {
        getJsonApiAsync(Utils.getUrl("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/reforgestones.json", new JsonObject()),
                data -> {
                    if (data != null) SkyhouseMod.INSTANCE.reforgeData = data;
                },
                e -> {
                    System.out.println("Error connecting to Moulberry's Github");
                    if (SkyhouseMod.INSTANCE.getListener().reforgesManuallyRefreshed)  {
                        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.RED + "Error connecting to Moulberry's Github"));
                    }
                });
    }

    public static String formatNumber(double value) {
        return df.format(value);
    }

    public static String fragType(String itemName) {
        if (bonzoFraggedItems.contains(itemName)) {
            return "BONZO_FRAGMENT";
        }
        if (scarfFraggedItems.contains(itemName)) {
            return "SCARF_FRAGMENT";
        }
        if (lividFraggedItems.contains(itemName)) {
            return "LIVID_FRAGMENT";
        }
        return null;
    }

    public static void renderItem(ItemStack itemStack, int x, int y) {
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        RenderHelper.enableGUIStandardItemLighting();
        renderItem.zLevel = -145;
        renderItem.renderItemAndEffectIntoGUI(itemStack, x, y);
        renderItem.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, itemStack, x, y, null);
        renderItem.zLevel = 0;
        RenderHelper.disableStandardItemLighting();
    }

    public static <O, T> Consumer<O> createConvertingCallback(Converter<O, T> converter, Consumer<T> target) {
        return ((O x) -> target.accept(converter.convert(x)));
    }

    public static Consumer<String> createStringToIntCallback(Consumer<Integer> target, int def) {
        Consumer<String> consumer = createConvertingCallback(x -> parseInt(x.replaceAll("\\s+", ""), def), target);
        return consumer;
    }

    public static String[] getLoreFromNBT(NBTTagCompound tag) {
        String[] lore = new String[0];
        NBTTagCompound display = tag.getCompoundTag("display");

        if (display.hasKey("Lore", 9)) {
            NBTTagList list = display.getTagList("Lore", 8);
            lore = new String[list.tagCount()];
            for (int i = 0; i < list.tagCount(); i++) {
                lore[i] = list.getStringTagAt(i);
            }
        }
        return lore;
    }

    //This was copy pasted from https://github.com/Mouberry/NotEnoughUpdates
    //to work with his lowest bin api (With a few changes to work here)
    public static String getInternalNameFromNBT(NBTTagCompound tag) {
        String internalName = null;
        if(tag != null && tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

            if(ea.hasKey("id", 8)) {
                internalName = ea.getString("id").replaceAll(":", "-");
            } else {
                return null;
            }

            if("PET".equals(internalName)) {
                String petInfo = ea.getString("petInfo");
                if(petInfo.length() > 0) {
                    JsonObject petInfoObject = SkyhouseMod.gson.fromJson(petInfo, JsonObject.class);
                    internalName = petInfoObject.get("type").getAsString();
                    String tier = petInfoObject.get("tier").getAsString();
                    switch(tier) {
                        case "COMMON":
                            internalName += ";0"; break;
                        case "UNCOMMON":
                            internalName += ";1"; break;
                        case "RARE":
                            internalName += ";2"; break;
                        case "EPIC":
                            internalName += ";3"; break;
                        case "LEGENDARY":
                            internalName += ";4"; break;
                        case "MYTHIC":
                            internalName += ";5"; break;
                    }
                }
            }
            if("ENCHANTED_BOOK".equals(internalName)) {
                NBTTagCompound enchants = ea.getCompoundTag("enchantments");

                for(String enchname : enchants.getKeySet()) {
                    internalName = enchname.toUpperCase() + ";" + enchants.getInteger(enchname);
                    break;
                }
            }
        }

        return internalName;
    }

    public static JsonObject nbtToJson(NBTTagCompound tag) {
        if (tag.getKeySet().size() == 0) return null;

        int id = tag.getShort("id");
        int damage = tag.getShort("Damage");
        int count = tag.getShort("Count");
        tag = tag.getCompoundTag("tag");

        if (id == 141) id = 391;


        NBTTagCompound display = tag.getCompoundTag("display");
        String[] lore = getLoreFromNBT(tag);

        Item mcItem = Item.getItemById(id);
        String itemid = "null";
        if (mcItem != null) {
            itemid = mcItem.getRegistryName();
        }
        String name = display.getString("Name");

        JsonObject item = new JsonObject();
        item.addProperty("itemid", itemid);
        item.addProperty("displayname", name);

        if (tag.hasKey("ExtraAttributes", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

            byte[] bytes = null;
            for (String key : ea.getKeySet()) {
                if (key.endsWith("_backpack_data") || key.equals("new_year_cake_bag_data")) {
                    bytes = ea.getByteArray(key);
                    break;
                }
            }
            if (bytes != null) {
                JsonArray bytesArr = new JsonArray();
                for (byte b : bytes) {
                    bytesArr.add(new JsonPrimitive(b));
                }
                item.add("item_contents", bytesArr);
            }
            if (ea.hasKey("dungeon_item_level")) {
                item.addProperty("dungeon_item_level", ea.getInteger("dungeon_item_level"));
            }
        }

        if (lore != null && lore.length > 0) {
            JsonArray jsonLore = new JsonArray();
            for (String line : lore) {
                jsonLore.add(new JsonPrimitive(line));
            }
            item.add("lore", jsonLore);
        }

        item.addProperty("damage", damage);
        if (count > 1) item.addProperty("count", count);
        item.addProperty("nbttag", tag.toString());

        return item;
    }

    public static JsonObject decodeItemBytes(String itemBytes) {
        try {
            NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(itemBytes)));
            return nbtToJson(tag.getTagList("i", Constants.NBT.TAG_COMPOUND).getCompoundTagAt(0));
        } catch (IOException ignored) {
            return null;
        }
    }

    public static ItemStack jsonToItem(JsonObject json) {
        if(json == null) return new ItemStack(Items.painting, 1, 10);

        ItemStack stack = new ItemStack(Item.itemRegistry.getObject(
                new ResourceLocation(json.get("itemid").getAsString())));

        if(json.has("count")) {
            stack.stackSize = json.get("count").getAsInt();
        }

        if(stack.getItem() == null) {
            stack = new ItemStack(Item.getItemFromBlock(Blocks.stone), 0, 255);
        } else {
            if(json.has("damage")) {
                stack.setItemDamage(json.get("damage").getAsInt());
            }

            if(json.has("nbttag")) {
                try {
                    NBTTagCompound tag = JsonToNBT.getTagFromJson(json.get("nbttag").getAsString());
                    stack.setTagCompound(tag);
                } catch(NBTException ignored) {
                }
            }
            if(json.has("lore")) {
                NBTTagCompound display = new NBTTagCompound();
                if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("display")) {
                    display = stack.getTagCompound().getCompoundTag("display");
                }
                display.setTag("Lore", getLore(json.get("lore").getAsJsonArray()));
                NBTTagCompound tag = stack.getTagCompound() != null ? stack.getTagCompound() : new NBTTagCompound();
                tag.setTag("display", display);
                stack.setTagCompound(tag);
            }
        }
        return stack;
    }


    public static NBTTagList getLore(JsonArray lore) {
        NBTTagList list = new NBTTagList();
        for (JsonElement lineObj : lore) {
            String line = lineObj.getAsString();
            list.appendTag(new NBTTagString(line));
        }
        return list;
    }

    public static boolean renderFlippingOverlay() {
        return SkyhouseMod.INSTANCE.getConfigManager().showFlippingOverlay;
    }

    public static boolean renderCreationOverlay() {
        return SkyhouseMod.INSTANCE.getConfigManager().showCreationOverlay;
    }

    public static int getGuiLeft() {
        int savedGuiLeft = SkyhouseMod.INSTANCE.getConfigManager().guiLeft;
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return SkyhouseMod.INSTANCE.getConfigManager().relativeGui ? Math.round(sr.getScaledWidth() * (((float) savedGuiLeft) / 1000)) : savedGuiLeft;
    }
    public static int getGuiTop() {
        int savedGuiTop = SkyhouseMod.INSTANCE.getConfigManager().guiTop;
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return SkyhouseMod.INSTANCE.getConfigManager().relativeGui ? Math.round(sr.getScaledHeight() * (((float) savedGuiTop) / 1000)) : savedGuiTop;
    }

    public static float getScaleFactor() {
        float savedSf = SkyhouseMod.INSTANCE.getConfigManager().guiScale;
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return SkyhouseMod.INSTANCE.getConfigManager().relativeGui ? (savedSf * sr.getScaledWidth()) / 255f : savedSf;

    }

}
