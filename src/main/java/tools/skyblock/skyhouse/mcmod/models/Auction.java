package tools.skyblock.skyhouse.mcmod.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.item.ItemStack;
import tools.skyblock.skyhouse.mcmod.util.Utils;

public class Auction {

    @SerializedName("item_name")
    @Expose
    private String name;
    @SerializedName("uuid")
    @Expose
    private String uuid;
    @SerializedName("price")
    @Expose
    private int price;
    @SerializedName("profit")
    @Expose
    private int profit;
    @SerializedName("item_bytes")
    @Expose
    private String nbt;

    private ItemStack stack;

    public void process() {
        stack = Utils.jsonToItem(Utils.decodeItemBytes(nbt));
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public int getPrice() {
        return price;
    }

    public int getProfit() {
        return profit;
    }

    public String getNbt() {
        return nbt;
    }

    public ItemStack getStack() {
        return stack;
    }
}