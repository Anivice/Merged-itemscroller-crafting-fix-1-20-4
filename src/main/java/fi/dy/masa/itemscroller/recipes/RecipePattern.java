package fi.dy.masa.itemscroller.recipes;

import java.util.HashSet;
import java.util.Arrays;
import javax.annotation.Nonnull;

import fi.dy.masa.itemscroller.compat.carpet.StackingShulkerBoxes;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.Constants;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RecipePattern
{
    private ItemStack result = InventoryUtils.EMPTY_STACK;
    private ItemStack[] recipe = new ItemStack[9];
    public CraftingRecipe cachedRecipeFromBook = null;
    public RecipeEntry<CraftingRecipe> cachedRecipeEntryFromBook = null;
    
    private int maxCraftAmount = 64;
    private HashSet<Item> recipeRemainders = new HashSet<Item>();

    public RecipePattern()
    {
        this.ensureRecipeSizeAndClearRecipe(9);
    }

    public void ensureRecipeSize(int size)
    {
        if (this.getRecipeLength() != size)
        {
            this.recipe = new ItemStack[size];
        }
    }

    public void clearRecipe()
    {
        Arrays.fill(this.recipe, InventoryUtils.EMPTY_STACK);
        this.result = InventoryUtils.EMPTY_STACK;
        this.cachedRecipeFromBook = null;
        this.maxCraftAmount = 64;
        this.recipeRemainders.clear();
    }

    public void ensureRecipeSizeAndClearRecipe(int size)
    {
        this.ensureRecipeSize(size);
        this.clearRecipe();
    }

    public void initializeRecipe() {
        for (int i = 0; i < this.recipe.length; i++) {
            if (this.recipe[i].getItem().hasRecipeRemainder()) {
                this.recipeRemainders.add(recipe[i].getItem().getRecipeRemainder());
            }
            int maxCount = StackingShulkerBoxes.getMaxCount(this.recipe[i]);
            if (maxCount < maxCraftAmount) {
                maxCraftAmount = maxCount;
            }
        }
    }

    public int getMaxCraftAmount() {
        return maxCraftAmount;
    }

    public void storeCraftingRecipe(Slot slot, HandledScreen<? extends ScreenHandler> gui, boolean clearIfEmpty)
    {
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

        if (range != null)
        {
            if (slot.hasStack())
            {
                int gridSize = range.getSlotCount();
                int numSlots = gui.getScreenHandler().slots.size();

                this.ensureRecipeSizeAndClearRecipe(gridSize);

                for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
                {
                    Slot slotTmp = gui.getScreenHandler().getSlot(s);
                    this.recipe[i] = slotTmp.hasStack() ? slotTmp.getStack().copy() : InventoryUtils.EMPTY_STACK;
                }

                this.result = slot.getStack().copy();
                this.initializeRecipe();
            }
            else if (clearIfEmpty)
            {
                this.clearRecipe();
            }
        }
    }

    public void copyRecipeFrom(RecipePattern other)
    {
        int size = other.getRecipeLength();
        ItemStack[] otherRecipe = other.getRecipeItems();

        this.ensureRecipeSizeAndClearRecipe(size);

        for (int i = 0; i < size; i++)
        {
            this.recipe[i] = InventoryUtils.isStackEmpty(otherRecipe[i]) == false ? otherRecipe[i].copy() : InventoryUtils.EMPTY_STACK;
        }

        this.result = InventoryUtils.isStackEmpty(other.getResult()) == false ? other.getResult().copy() : InventoryUtils.EMPTY_STACK;
        this.initializeRecipe();
    }

    public void readFromNBT(@Nonnull NbtCompound nbt)
    {
        if (nbt.contains("Result", Constants.NBT.TAG_COMPOUND) && nbt.contains("Ingredients", Constants.NBT.TAG_LIST))
        {
            NbtList tagIngredients = nbt.getList("Ingredients", Constants.NBT.TAG_COMPOUND);
            int count = tagIngredients.size();
            int length = nbt.getInt("Length");

            if (length > 0)
            {
                this.ensureRecipeSizeAndClearRecipe(length);
            }

            for (int i = 0; i < count; i++)
            {
                NbtCompound tag = tagIngredients.getCompound(i);
                int slot = tag.getInt("Slot");

                if (slot >= 0 && slot < this.recipe.length)
                {
                    this.recipe[slot] = ItemStack.fromNbt(tag);
                }
            }

            this.result = ItemStack.fromNbt(nbt.getCompound("Result"));
            this.initializeRecipe();
        }
    }

    @Nonnull
    public NbtCompound writeToNBT(@Nonnull NbtCompound nbt)
    {
        if (this.isValid())
        {
            NbtCompound tag = new NbtCompound();
            this.result.writeNbt(tag);

            nbt.putInt("Length", this.recipe.length);
            nbt.put("Result", tag);

            NbtList tagIngredients = new NbtList();

            for (int i = 0; i < this.recipe.length; i++)
            {
                if (InventoryUtils.isStackEmpty(this.recipe[i]) == false)
                {
                    tag = new NbtCompound();
                    tag.putInt("Slot", i);
                    this.recipe[i].writeNbt(tag);
                    tagIngredients.add(tag);
                }
            }

            nbt.put("Ingredients", tagIngredients);
        }

        return nbt;
    }

    public ItemStack getResult()
    {
        return this.result;
    }

    public int getRecipeLength()
    {
        return this.recipe.length;
    }

    public ItemStack[] getRecipeItems()
    {
        return this.recipe;
    }

    public HashSet<Item> getRecipeRemainders()
    {
        return this.recipeRemainders;
    }

    public boolean isValid()
    {
        return InventoryUtils.isStackEmpty(this.getResult()) == false;
    }
}
