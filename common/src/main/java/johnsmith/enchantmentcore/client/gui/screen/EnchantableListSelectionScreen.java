package johnsmith.enchantmentcore.client.gui.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import johnsmith.enchantmentcore.config.property.EnchantableItemListProperty;
import johnsmith.enchantmentcore.api.config.data.ItemOrItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnchantableListSelectionScreen extends Screen {
    private static final ResourceLocation SELECT_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    private static final ResourceLocation SELECT = ResourceLocation.withDefaultNamespace("transferable_list/select");
    private static final ResourceLocation UNSELECT_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted");
    private static final ResourceLocation UNSELECT = ResourceLocation.withDefaultNamespace("transferable_list/unselect");
    private static final ResourceLocation MOVE_UP = ResourceLocation.withDefaultNamespace("transferable_list/move_up");
    private static final ResourceLocation MOVE_UP_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final ResourceLocation MOVE_DOWN = ResourceLocation.withDefaultNamespace("transferable_list/move_down");
    private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");

    private final Screen parent;
    private final Consumer<List<ItemOrItems>> onSelect;
    private final List<ItemOrItems> selectedItems;
    private final List<ItemOrItems> allAvailableItems;

    private EditBox availableSearchBox;
    private EditBox selectedSearchBox;
    private ElementList availableList;
    private ElementList selectedList;
    private List<Component> deferredTooltip;

    public EnchantableListSelectionScreen(Screen parent, Component title, List<ItemOrItems> initialSelection, Consumer<List<ItemOrItems>> onSelect) {
        super(title);
        this.parent = parent;
        this.onSelect = onSelect;
        this.selectedItems = new ArrayList<>(initialSelection);
        this.allAvailableItems = new ArrayList<>();

        // 1. Populate Enchantable Tags
        BuiltInRegistries.ITEM.getTagNames()
                .filter(key -> key.location().getPath().startsWith("enchantable/"))
                .forEach(key -> this.allAvailableItems.add(new ItemOrItems(key)));

        // 2. Populate Enchantable Items
        BuiltInRegistries.ITEM.stream()
                .filter(EnchantableItemListProperty::isItemEnchantable)
                .forEach(item -> this.allAvailableItems.add(new ItemOrItems(item)));
    }

    @Override
    protected void init() {
        int halfWidth = this.width / 2 - 12;

        this.availableList = new ElementList(this.minecraft, halfWidth, this.height - 84, 24, 36, 8, Component.translatable("pack.available.title"));
        this.addRenderableWidget(this.availableList);

        this.selectedList = new ElementList(this.minecraft, halfWidth, this.height - 84, 24, 36, this.width / 2 + 4, Component.translatable("pack.selected.title"));
        this.addRenderableWidget(this.selectedList);

        this.availableSearchBox = this.addSearchBar(8, this.height - 52, halfWidth, query -> {
            this.updateAvailable(query);
            this.availableList.setScrollAmount(0);
        });
        this.selectedSearchBox = this.addSearchBar(this.width / 2 + 4, this.height - 52, halfWidth, query -> {
            this.updateSelected(query);
            this.selectedList.setScrollAmount(0);
        });

        this.refreshLists();

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 154, this.height - 26, 150, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            this.onSelect.accept(new ArrayList<>(this.selectedItems));
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 + 4, this.height - 26, 150, 20).build());
    }

    private EditBox addSearchBar(int x, int y, int totalWidth, Consumer<String> responder) {
        int clearWidth = 20;
        int boxWidth = totalWidth - clearWidth - 4;

        EditBox searchBox = new EditBox(this.font, x, y, boxWidth, 20, Component.empty());
        searchBox.setHint(Component.translatable("config_overhauled.config.search_by_name_or_namespace").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        searchBox.setResponder(responder);
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> {
            searchBox.setValue("");
            searchBox.setFocused(true);
        }).bounds(x + boxWidth + 4, y, clearWidth, 20).build());

        return searchBox;
    }

    private void refreshLists() {
        this.updateAvailable(this.availableSearchBox.getValue());
        this.updateSelected(this.selectedSearchBox.getValue());
    }

    private void updateAvailable(String query) {
        double scroll = this.availableList.getScrollAmount();
        this.availableList.clearEntries();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (ItemOrItems element : this.allAvailableItems) {
            if (this.selectedItems.contains(element)) continue;

            String searchableText = element.asString().toLowerCase(Locale.ROOT);
            if (lowerQuery.isEmpty() || searchableText.contains(lowerQuery)) {
                this.availableList.addEntry(new ElementEntry(this.availableList, element, SELECT, SELECT_HIGHLIGHTED, () -> {
                    this.selectedItems.add(element);
                    this.refreshLists();
                }));
            }
        }
        this.availableList.setScrollAmount(scroll);
    }

    private void updateSelected(String query) {
        double scroll = this.selectedList.getScrollAmount();
        this.selectedList.clearEntries();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (int i = 0; i < this.selectedItems.size(); i++) {
            ItemOrItems element = this.selectedItems.get(i);

            String searchableText = element.asString().toLowerCase(Locale.ROOT);
            if (lowerQuery.isEmpty() || searchableText.contains(lowerQuery)) {
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
        }
        this.selectedList.setScrollAmount(scroll);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.deferredTooltip = null;

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        if (this.deferredTooltip != null) {
            guiGraphics.renderTooltip(this.font, this.deferredTooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private Component trimComponent(Component component, int maxWidth) {
        if (this.font.width(component) <= maxWidth) return component;
        int ellipsisWidth = this.font.width("...");
        return Component.literal(this.font.plainSubstrByWidth(component.getString(), maxWidth - ellipsisWidth) + "...").withStyle(component.getStyle());
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
        private final ItemOrItems element;
        private final Component displayName;
        private final String subText;
        private final ItemStack icon;
        private final ResourceLocation sprite;
        private final ResourceLocation highlightedSprite;
        private final Runnable onTransfer;

        private Runnable onMoveUp;
        private Runnable onMoveDown;
        private List<Component> tooltipCache;

        public ElementEntry(ElementList list, ItemOrItems element, ResourceLocation sprite, ResourceLocation highlightedSprite, Runnable onTransfer) {
            this.list = list;
            this.element = element;
            this.sprite = sprite;
            this.highlightedSprite = highlightedSprite;
            this.onTransfer = onTransfer;

            if (element.isTag()) {
                this.icon = new ItemStack(Items.NAME_TAG);
                this.displayName = Component.literal(element.asString()).withStyle(ChatFormatting.YELLOW);
                this.subText = "Item Tag";
            } else {
                this.icon = new ItemStack(element.getItem());
                this.displayName = element.getItem().getDescription();
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(element.getItem());
                this.subText = key != null ? key.toString() : "";
            }
        }

        public void setSortable(Runnable onMoveUp, Runnable onMoveDown) {
            this.onMoveUp = onMoveUp;
            this.onMoveDown = onMoveDown;
        }

        private List<Component> getTooltip() {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(this.displayName.copy().withStyle(ChatFormatting.YELLOW));
            if (this.element.isTag()) {
                Optional<HolderSet.Named<Item>> tagSet = BuiltInRegistries.ITEM.getTag(this.element.getTag());
                if (tagSet.isPresent()) {
                    int count = 0;
                    int maxDisplay = 8;
                    for (Holder<Item> holder : tagSet.get()) {
                        if (count < maxDisplay) {
                            tooltip.add(Component.literal("   ").withStyle(ChatFormatting.DARK_GRAY)
                                    .append(holder.value().getDescription().copy().withStyle(ChatFormatting.GRAY)));
                        }
                        count++;
                    }
                    if (count > maxDisplay) {
                        tooltip.add(Component.literal("   ... and " + (count - maxDisplay) + " more")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    }
                } else {
                    tooltip.add(Component.literal("Empty Tag").withStyle(ChatFormatting.RED));
                }
            }
            return tooltip;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            if (isMouseOver) guiGraphics.fill(left, top, left + 32, top + 32, 0x99999999); // -1601138544 Equivalent

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
                    guiGraphics.blitSprite(relativeX < 32 ? this.highlightedSprite : this.sprite, left, top, 32, 32);
                } else {
                    guiGraphics.blitSprite(relativeX < 16 ? this.highlightedSprite : this.sprite, left, top, 32, 32);
                    if (this.onMoveUp != null) guiGraphics.blitSprite(relativeX < 32 && relativeX > 16 && relativeY < 16 ? MOVE_UP_HIGHLIGHTED : MOVE_UP, left, top, 32, 32);
                    if (this.onMoveDown != null) guiGraphics.blitSprite(relativeX < 32 && relativeX > 16 && relativeY > 16 ? MOVE_DOWN_HIGHLIGHTED : MOVE_DOWN, left, top, 32, 32);
                }
                guiGraphics.pose().popPose();

                if (relativeX > 32 && this.element.isTag()) {
                    EnchantableListSelectionScreen.this.deferredTooltip = this.getTooltip();
                }
            }

            guiGraphics.drawString(EnchantableListSelectionScreen.this.font, trimComponent(this.displayName, 203), left + 34, top + 1, 0xFFFFFFFF, false);
            guiGraphics.drawString(EnchantableListSelectionScreen.this.font, this.subText, left + 34, top + 12, 0xFF888888, false);
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

}