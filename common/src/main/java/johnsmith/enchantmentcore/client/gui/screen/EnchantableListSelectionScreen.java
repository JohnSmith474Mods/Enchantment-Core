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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnchantableListSelectionScreen extends Screen {
    private static final Identifier SELECT_HIGHLIGHTED = Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    private static final Identifier SELECT = Identifier.withDefaultNamespace("transferable_list/select");
    private static final Identifier UNSELECT_HIGHLIGHTED = Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");
    private static final Identifier UNSELECT = Identifier.withDefaultNamespace("transferable_list/unselect");
    private static final Identifier MOVE_UP = Identifier.withDefaultNamespace("transferable_list/move_up");
    private static final Identifier MOVE_UP_HIGHLIGHTED = Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final Identifier MOVE_DOWN = Identifier.withDefaultNamespace("transferable_list/move_down");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED = Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");

    private final Screen parent;
    private final Consumer<List<ItemOrItems>> onSelect;
    private final List<ItemOrItems> selectedItems;
    private final List<ItemOrItems> allAvailableItems;

    private EditBox availableSearchBox;
    private EditBox selectedSearchBox;
    private ElementList availableList;
    private ElementList selectedList;

    public EnchantableListSelectionScreen(Screen parent, Component title, List<ItemOrItems> initialSelection, Consumer<List<ItemOrItems>> onSelect) {
        super(title);
        this.parent = parent;
        this.onSelect = onSelect;
        this.selectedItems = new ArrayList<>(initialSelection);
        this.allAvailableItems = new ArrayList<>();

        BuiltInRegistries.ITEM.getTags()
                .filter(key -> key.key().location().getPath().startsWith("enchantable/"))
                .forEach(key -> this.allAvailableItems.add(new ItemOrItems(key.key())));

        BuiltInRegistries.ITEM.stream()
                .filter(EnchantableItemListProperty::isItemEnchantable)
                .forEach(item -> this.allAvailableItems.add(new ItemOrItems(item)));
    }

    @Override
    protected void init() {
        int halfWidth = this.width / 2 - 12;

        this.availableList = new ElementList(this.minecraft, halfWidth, this.height - 84, 24, 36, 8);
        this.addRenderableWidget(this.availableList);

        this.selectedList = new ElementList(this.minecraft, halfWidth, this.height - 84, 24, 36, this.width / 2 + 4);
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
        double scroll = this.availableList.scrollAmount();
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
        double scroll = this.selectedList.scrollAmount();
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        this.drawListHeader(guiGraphics, this.availableList, Component.translatable("pack.available.title"));
        this.drawListHeader(guiGraphics, this.selectedList, Component.translatable("pack.selected.title"));
    }

    private void drawListHeader(GuiGraphics guiGraphics, ElementList list, Component title) {
        Component formattedTitle = title.copy()
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.UNDERLINE);

        int headerY = list.getY() - 16;
        guiGraphics.drawCenteredString(this.minecraft.font, formattedTitle, list.getX() + list.getRowWidth() / 2, headerY + 2, 0xFFFFFFFF);
    }

    private Component trimComponent(Component component, int maxWidth) {
        if (this.font.width(component) <= maxWidth) return component;
        int ellipsisWidth = this.font.width("...");
        return Component.literal(this.font.plainSubstrByWidth(component.getString(), maxWidth - ellipsisWidth) + "...").withStyle(component.getStyle());
    }

    private class ElementList extends ObjectSelectionList<ElementEntry> {

        public ElementList(Minecraft minecraft, int width, int height, int y, int itemHeight, int x) {
            super(minecraft, width, height, y, itemHeight);
            this.setX(x);
        }

        public void clearEntries() { super.clearEntries(); }
        public int addEntry(ElementEntry entry) { return super.addEntry(entry); }
        @Override public int getRowTop(int index) { return super.getRowTop(index); }
        @Override public int getRowWidth() { return this.width - 20; }
        @Override protected int scrollBarX() { return this.getX() + this.width - 6; }
    }

    private class ElementEntry extends ObjectSelectionList.Entry<ElementEntry> {
        private final ElementList list;
        private final ItemOrItems element;
        private final Component displayName;
        private final String subText;
        private final ItemStack icon;
        private final Identifier sprite;
        private final Identifier highlightedSprite;
        private final Runnable onTransfer;

        private Runnable onMoveUp;
        private Runnable onMoveDown;

        public ElementEntry(ElementList list, ItemOrItems element, Identifier sprite, Identifier highlightedSprite, Runnable onTransfer) {
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
                this.displayName = element.getItem().getName();
                Identifier key = BuiltInRegistries.ITEM.getKey(element.getItem());
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
                Optional<HolderSet.Named<Item>> tagSet = BuiltInRegistries.ITEM.get(this.element.getTag());
                if (tagSet.isPresent()) {
                    int count = 0;
                    int maxDisplay = 8;
                    for (Holder<Item> holder : tagSet.get()) {
                        if (count < maxDisplay) {
                            tooltip.add(Component.literal("   ").withStyle(ChatFormatting.DARK_GRAY)
                                    .append(holder.value().getName().copy().withStyle(ChatFormatting.GRAY)));
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
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int left = this.getX() + 2;
            int top = this.getY() + 2;

            if (isHovering) {
                guiGraphics.fill(left, top, left + 32, top + 32, -1601138544);
            }

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)left, (float)top);
            guiGraphics.pose().scale(2.0F, 2.0F);
            guiGraphics.renderItem(this.icon, 0, 0);
            guiGraphics.pose().popMatrix();

            if (isHovering) {
                int relativeX = mouseX - left;
                int relativeY = mouseY - top;

                guiGraphics.pose().pushMatrix();
                guiGraphics.nextStratum();

                if (this.onMoveUp == null && this.onMoveDown == null) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, relativeX < 32 ? this.highlightedSprite : this.sprite, left, top, 32, 32);
                } else {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, relativeX < 16 ? this.highlightedSprite : this.sprite, left, top, 32, 32);
                    if (this.onMoveUp != null) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, relativeX < 32 && relativeX > 16 && relativeY < 16 ? MOVE_UP_HIGHLIGHTED : MOVE_UP, left, top, 32, 32);
                    if (this.onMoveDown != null) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, relativeX < 32 && relativeX > 16 && relativeY > 16 ? MOVE_DOWN_HIGHLIGHTED : MOVE_DOWN, left, top, 32, 32);
                }
                guiGraphics.pose().popMatrix();

                if (relativeX > 32 && this.element.isTag()) {
                    guiGraphics.setTooltipForNextFrame(EnchantableListSelectionScreen.this.font, this.getTooltip(), Optional.empty(), mouseX, mouseY);
                }
            }

            guiGraphics.drawString(EnchantableListSelectionScreen.this.font, trimComponent(this.displayName, 203), left + 34, top + 1, 0xFFFFFFFF, false);
            guiGraphics.drawString(EnchantableListSelectionScreen.this.font, this.subText, left + 34, top + 12, 0xFF888888, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
            double j = event.x() - (double) this.list.getRowLeft();
            double k = event.y() - (double) this.list.getRowTop(this.list.children().indexOf(this));

            if (j <= 32.0D) {
                if (this.onMoveUp == null && this.onMoveDown == null) {
                    if (this.onTransfer != null) this.onTransfer.run();
                    return true;
                }
                if (j < 16.0D) {
                    if (this.onTransfer != null) this.onTransfer.run();
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
            return super.mouseClicked(event, pressed);
        }

        @Override
        public Component getNarration() {
            return this.displayName;
        }
    }
}