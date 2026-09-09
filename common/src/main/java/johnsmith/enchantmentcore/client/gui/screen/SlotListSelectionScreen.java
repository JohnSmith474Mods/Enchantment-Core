package johnsmith.enchantmentcore.client.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SlotListSelectionScreen extends Screen {
    private static final ResourceLocation SELECT_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    private static final ResourceLocation SELECT = ResourceLocation.withDefaultNamespace("transferable_list/select");
    private static final ResourceLocation UNSELECT_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted");
    private static final ResourceLocation UNSELECT = ResourceLocation.withDefaultNamespace("transferable_list/unselect");
    private static final ResourceLocation MOVE_UP = ResourceLocation.withDefaultNamespace("transferable_list/move_up");
    private static final ResourceLocation MOVE_UP_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final ResourceLocation MOVE_DOWN = ResourceLocation.withDefaultNamespace("transferable_list/move_down");
    private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");

    private final Screen parent;
    private final Consumer<List<EquipmentSlotGroup>> onSelect;
    private final List<EquipmentSlotGroup> selectedItems;
    private final List<EquipmentSlotGroup> allAvailableItems;

    private ElementList availableList;
    private ElementList selectedList;

    public SlotListSelectionScreen(Screen parent, Component title, List<EquipmentSlotGroup> initialSelection, Consumer<List<EquipmentSlotGroup>> onSelect) {
        super(title);
        this.parent = parent;
        this.onSelect = onSelect;
        this.selectedItems = new ArrayList<>(initialSelection);

        this.allAvailableItems = Arrays.asList(EquipmentSlotGroup.values());
    }

    @Override
    protected void init() {
        int halfWidth = this.width / 2 - 12;

        this.availableList = new ElementList(this.minecraft, halfWidth, this.height - 64, 24, 36, 8, Component.literal("Available Slots"));
        this.addRenderableWidget(this.availableList);

        this.selectedList = new ElementList(this.minecraft, halfWidth, this.height - 64, 24, 36, this.width / 2 + 4, Component.literal("Selected Slots"));
        this.addRenderableWidget(this.selectedList);

        this.refreshLists();

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 154, this.height - 26, 150, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            this.onSelect.accept(new ArrayList<>(this.selectedItems));
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 + 4, this.height - 26, 150, 20).build());
    }

    private void refreshLists() {
        double availableScroll = this.availableList.getScrollAmount();
        this.availableList.clearEntries();
        for (EquipmentSlotGroup element : this.allAvailableItems) {
            if (!this.selectedItems.contains(element)) {
                this.availableList.addEntry(new ElementEntry(this.availableList, element, SELECT, SELECT_HIGHLIGHTED, () -> {
                    this.selectedItems.add(element);
                    this.refreshLists();
                }));
            }
        }
        this.availableList.setScrollAmount(availableScroll);

        double selectedScroll = this.selectedList.getScrollAmount();
        this.selectedList.clearEntries();
        for (int i = 0; i < this.selectedItems.size(); i++) {
            EquipmentSlotGroup element = this.selectedItems.get(i);
            ElementEntry entry = new ElementEntry(this.selectedList, element, UNSELECT, UNSELECT_HIGHLIGHTED, () -> {
                this.selectedItems.remove(element);
                this.refreshLists();
            });

            int index = i;
            Runnable onMoveUp = index > 0 ? () -> {
                this.selectedItems.remove(index);
                this.selectedItems.add(index - 1, element);
                this.refreshLists();
            } : null;

            Runnable onMoveDown = index < this.selectedItems.size() - 1 ? () -> {
                this.selectedItems.remove(index);
                this.selectedItems.add(index + 1, element);
                this.refreshLists();
            } : null;

            entry.setSortable(onMoveUp, onMoveDown);
            this.selectedList.addEntry(entry);
        }
        this.selectedList.setScrollAmount(selectedScroll);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    private class ElementList extends ObjectSelectionList<ElementEntry> {
        private final Component listTitle;

        public ElementList(Minecraft minecraft, int width, int height, int y, int itemHeight, int x, Component listTitle) {
            super(minecraft, width, height, y, itemHeight);
            this.setX(x);
            this.listTitle = listTitle;
            this.setRenderHeader(true, 16);
        }

        public void clearEntries() { super.clearEntries(); }
        public int addEntry(ElementEntry entry) { return super.addEntry(entry); }
        @Override public int getRowTop(int index) { return super.getRowTop(index); }
        @Override public int getRowWidth() { return this.width - 20; }
        @Override protected int getScrollbarPosition() { return this.getX() + this.width - 6; }

        @Override
        protected void renderHeader(GuiGraphics guiGraphics, int x, int y) {
            Component formattedTitle = this.listTitle.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE);
            guiGraphics.drawCenteredString(this.minecraft.font, formattedTitle, x + this.getRowWidth() / 2, y + 2, 0xFFFFFF);
        }
    }

    private class ElementEntry extends ObjectSelectionList.Entry<ElementEntry> {
        private final ElementList list;
        private final EquipmentSlotGroup element;
        private final Component displayName;
        private final ResourceLocation sprite;
        private final ResourceLocation highlightedSprite;
        private final Runnable onTransfer;
        private final ItemStack icon; // Add icon field

        private Runnable onMoveUp;
        private Runnable onMoveDown;

        public ElementEntry(ElementList list, EquipmentSlotGroup element, ResourceLocation sprite, ResourceLocation highlightedSprite, Runnable onTransfer) {
            this.list = list;
            this.element = element;
            this.sprite = sprite;
            this.highlightedSprite = highlightedSprite;
            this.onTransfer = onTransfer;

            this.displayName = Component.literal(element.getSerializedName().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.YELLOW);
            this.icon = getIconForGroup(element); // Resolve icon on construction
        }

        public void setSortable(Runnable onMoveUp, Runnable onMoveDown) {
            this.onMoveUp = onMoveUp;
            this.onMoveDown = onMoveDown;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            if (isMouseOver) {
                guiGraphics.fill(left, top, left + 32, top + 32, 0x99999999);
            }

            // Render the representative item icon
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left, top, 0);
            guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);
            guiGraphics.renderItem(this.icon, 0, 0);
            guiGraphics.pose().popPose();

            if (isMouseOver) {
                int relativeX = mouseX - left;
                int relativeY = mouseY - top;

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 200.0F);

                if (this.onMoveUp == null && this.onMoveDown == null) {
                    guiGraphics.blitSprite(RenderType::guiTextured, relativeX < 32 ? this.highlightedSprite : this.sprite, left, top, 32, 32);
                } else {
                    guiGraphics.blitSprite(RenderType::guiTextured, relativeX < 16 ? this.highlightedSprite : this.sprite, left, top, 32, 32);
                    if (this.onMoveUp != null) guiGraphics.blitSprite(RenderType::guiTextured, relativeX < 32 && relativeX > 16 && relativeY < 16 ? MOVE_UP_HIGHLIGHTED : MOVE_UP, left, top, 32, 32);
                    if (this.onMoveDown != null) guiGraphics.blitSprite(RenderType::guiTextured, relativeX < 32 && relativeX > 16 && relativeY > 16 ? MOVE_DOWN_HIGHLIGHTED : MOVE_DOWN, left, top, 32, 32);
                }
                guiGraphics.pose().popPose();
            }

            // Shift the text rendering over to account for the 32x32 icon
            guiGraphics.drawString(SlotListSelectionScreen.this.font, this.displayName, left + 36, top + 12, 0xFFFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double j = mouseX - (double) this.list.getRowLeft();
            double k = mouseY - (double) this.list.getRowTop(this.list.children().indexOf(this));

            if (j <= 32.0D) {
                if (this.onMoveUp == null && this.onMoveDown == null) {
                    this.onTransfer.run();
                    return true;
                }
                if (j < 16.0D) {
                    this.onTransfer.run();
                    return true;
                }
                if (j > 16.0D && k < 16.0D && this.onMoveUp != null) {
                    this.onMoveUp.run();
                    return true;
                }
                if (j > 16.0D && k > 16.0D && this.onMoveDown != null) {
                    this.onMoveDown.run();
                    return true;
                }
            }
            this.list.setSelected(this);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return this.displayName;
        }
    }

    private static ItemStack getIconForGroup(EquipmentSlotGroup group) {
        return switch (group) {
            case ANY -> new ItemStack(Items.NETHER_STAR);
            case MAINHAND -> new ItemStack(Items.IRON_SWORD);
            case OFFHAND -> new ItemStack(Items.SHIELD);
            case HAND -> new ItemStack(Items.IRON_PICKAXE);
            case ARMOR -> new ItemStack(Items.ARMOR_STAND);
            case HEAD -> new ItemStack(Items.IRON_HELMET);
            case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
            case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
            case FEET -> new ItemStack(Items.IRON_BOOTS);
            case BODY -> new ItemStack(Items.IRON_HORSE_ARMOR);
        };
    }
}