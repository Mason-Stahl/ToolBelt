package dev.gigaherz.toolbelt.client.radial;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.gigaherz.toolbelt.client.ClientConfig;
import dev.gigaherz.toolbelt.client.InputHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A ring of selectable items around the screen center. Items are laid out clockwise starting
 * at {@link #START_ANGLE}; the inner {@link #radiusIn} is a dead zone where nothing is hovered.
 */
public class GenericRadialMenu
{
    public static final float OPEN_ANIMATION_LENGTH = 2.5f;

    /** Angle (radians, screen space, y down) of the center of the first item: straight up. */
    public static final double START_ANGLE = 1.5 * Math.PI;

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final float PRECISION = 2.5f / 360.0f;
    private static final float INNER_RADIUS = 30;

    public enum State
    {
        INITIALIZING,
        OPENING,
        NORMAL,
        CLOSING,
        CLOSED
    }

    public final IRadialMenuHost host;
    private final Minecraft minecraft;
    private final List<RadialMenuItem> items = new ArrayList<>();
    private final List<RadialMenuItem> visibleItems = new ArrayList<>();

    public int backgroundColor = 0x3F000000;
    public int backgroundColorHover = 0x3FFFFFFF;

    private State state = State.INITIALIZING;
    private double startAnimation;
    private float animProgress;
    private float radiusIn;
    private float radiusOut;
    private float itemRadius;

    private @Nullable Component centralText;

    public GenericRadialMenu(Minecraft minecraft, IRadialMenuHost host)
    {
        this.minecraft = minecraft;
        this.host = host;
    }

    public void setCentralText(@Nullable Component centralText)
    {
        this.centralText = centralText;
    }

    public int getHovered()
    {
        for (int i = 0; i < visibleItems.size(); i++)
        {
            if (visibleItems.get(i).isHovered())
                return i;
        }
        return -1;
    }

    public @Nullable RadialMenuItem getHoveredItem()
    {
        for (RadialMenuItem item : visibleItems)
        {
            if (item.isHovered())
                return item;
        }
        return null;
    }

    public void setHovered(int which)
    {
        for (int i = 0; i < visibleItems.size(); i++)
        {
            visibleItems.get(i).setHovered(i == which);
        }
    }

    public int getVisibleItemCount()
    {
        return visibleItems.size();
    }

    public void clickItem()
    {
        if (state == State.NORMAL)
        {
            RadialMenuItem item = getHoveredItem();
            if (item != null)
            {
                item.onClick();
                return;
            }
        }
        onClickOutside();
    }

    /** Called when a click doesn't land on an item. */
    public void onClickOutside()
    {
    }

    public boolean isClosed()
    {
        return state == State.CLOSED;
    }

    public boolean isReady()
    {
        return state == State.NORMAL;
    }

    void visibilityChanged()
    {
        visibleItems.clear();
        for (RadialMenuItem item : items)
        {
            if (item.isVisible())
                visibleItems.add(item);
        }
    }

    public void add(RadialMenuItem item)
    {
        items.add(item);
        if (item.isVisible())
            visibleItems.add(item);
    }

    public void addAll(Collection<? extends RadialMenuItem> newItems)
    {
        for (RadialMenuItem item : newItems)
            add(item);
    }

    public void clear()
    {
        items.clear();
        visibleItems.clear();
    }

    private double now()
    {
        if (minecraft.level == null)
            return 0;
        return minecraft.level.getGameTime() + (double) minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public void close()
    {
        if (state == State.CLOSING || state == State.CLOSED)
            return;
        state = minecraft.level == null ? State.CLOSED : State.CLOSING;
        startAnimation = now();
        animProgress = 1.0f;
        setHovered(-1);
    }

    public void tick()
    {
        if (state == State.INITIALIZING)
        {
            startAnimation = now();
            state = State.OPENING;
            animProgress = 0;
        }

        for (RadialMenuItem item : visibleItems)
            item.tick();
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY)
    {
        updateAnimationState(partialTicks);

        if (isClosed())
            return;

        Screen owner = host.getScreen();
        Font font = host.getFontRenderer();

        boolean animated = state == State.OPENING || state == State.CLOSING;
        radiusIn = animated ? Math.max(0.1f, INNER_RADIUS * animProgress) : INNER_RADIUS;
        radiusOut = radiusIn * 2;
        itemRadius = (radiusIn + radiusOut) * 0.5f;
        float animTop = animated ? (1 - animProgress) * owner.height / 2.0f : 0;

        if (isReady())
            processMouse(mouseX, mouseY);

        int x = owner.width / 2;
        int y = owner.height / 2;

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(0, animTop);
        extractBackground(graphics, x, y, partialTicks);
        pose.popMatrix();

        if (!isReady())
            return;

        extractItems(graphics, x, y, owner.width, owner.height, font);

        Component currentCentralText = centralText;
        RadialMenuItem hovered = getHoveredItem();
        if (hovered != null && hovered.getCentralText() != null)
            currentCentralText = hovered.getCentralText();

        if (currentCentralText != null)
        {
            String text = currentCentralText.getString();
            float textX = (owner.width - font.width(text)) / 2.0f;
            float textY = (owner.height - font.lineHeight) / 2.0f;
            graphics.text(font, text, (int) textX, (int) textY, 0xFFFFFFFF, true);
        }

        if (hovered != null)
            hovered.prepareTooltip(new DrawingContext(graphics, owner.width, owner.height, mouseX, mouseY, font));
    }

    private void updateAnimationState(float partialTicks)
    {
        if (minecraft.level == null)
        {
            state = State.CLOSED;
            return;
        }

        double elapsed = (minecraft.level.getGameTime() + partialTicks - startAnimation) / OPEN_ANIMATION_LENGTH;
        switch (state)
        {
            case OPENING ->
            {
                animProgress = (float) elapsed;
                if (animProgress >= 1.0f || getVisibleItemCount() == 0)
                {
                    animProgress = 1;
                    state = State.NORMAL;
                }
            }
            case CLOSING ->
            {
                animProgress = 1 - (float) elapsed;
                if (animProgress <= 0 || getVisibleItemCount() == 0)
                {
                    animProgress = 0;
                    state = State.CLOSED;
                }
            }
            default ->
            {
            }
        }
    }

    private void extractItems(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Font font)
    {
        int numItems = visibleItems.size();
        for (int i = 0; i < numItems; i++)
        {
            float angle = (float) getAngleFor(i, numItems);
            float posX = x + itemRadius * Mth.cos(angle);
            float posY = y + itemRadius * Mth.sin(angle);
            visibleItems.get(i).extractRenderState(new DrawingContext(graphics, width, height, posX, posY, font));
        }
    }

    private void extractBackground(GuiGraphicsExtractor graphics, float x, float y, float partialTick)
    {
        int numItems = visibleItems.size();
        for (int i = 0; i < numItems; i++)
        {
            RadialMenuItem item = visibleItems.get(i);
            float s = (float) getAngleFor(i - 0.5, numItems);
            float e = (float) getAngleFor(i + 0.5, numItems);

            float hoverState = item.getHoverState(partialTick);
            int color = ARGB.srgbLerp(hoverState, backgroundColor, backgroundColorHover);

            if (ClientConfig.minecraftHasNoCircles)
            {
                float middle = (s + e) / 2;
                float size = 10.0f + 2.0f * hoverState;
                float cx = x + Mth.cos(middle) * itemRadius;
                float cy = y + Mth.sin(middle) * itemRadius;
                graphics.fill(Mth.floor(cx - size), Mth.floor(cy - size), Mth.ceil(cx + size), Mth.ceil(cy + size), color);
            }
            else
            {
                float radiusExpand = 4.0f * hoverState;
                graphics.guiRenderState.addGuiElement(new PieArc(
                        RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                        x, y, radiusIn, radiusOut + radiusExpand, s, e, color));
            }
        }
    }

    public void cyclePrevious()
    {
        int numItems = getVisibleItemCount();
        if (numItems == 0)
            return;
        int which = getHovered() - 1;
        if (which < 0)
            which = numItems - 1;
        setHovered(which);
        moveMouseToItem(which, numItems);
    }

    public void cycleNext()
    {
        int numItems = getVisibleItemCount();
        if (numItems == 0)
            return;
        int which = getHovered() + 1;
        if (which >= numItems)
            which = 0;
        setHovered(which);
        moveMouseToItem(which, numItems);
    }

    private void moveMouseToItem(int which, int numItems)
    {
        Screen owner = host.getScreen();
        double angle = getAngleFor(which, numItems);
        InputHelper.setGuiMousePosition(
                owner.width / 2.0 + itemRadius * Math.cos(angle),
                owner.height / 2.0 + itemRadius * Math.sin(angle));
    }

    private void processMouse(int mouseX, int mouseY)
    {
        int numItems = getVisibleItemCount();

        Screen owner = host.getScreen();
        int x = owner.width / 2;
        int y = owner.height / 2;
        double a = Math.atan2(mouseY - y, mouseX - x);
        double d = Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2));

        int hovered = -1;
        if (numItems > 0)
        {
            // Bring the mouse angle into the range covered by the wedges.
            double s0 = getAngleFor(-0.5, numItems);
            double s1 = getAngleFor(numItems - 0.5, numItems);
            while (a < s0)
                a += TWO_PI;
            while (a >= s1)
                a -= TWO_PI;

            boolean inReach = d >= radiusIn && (d < radiusOut || ClientConfig.clipMouseToCircle || ClientConfig.allowClickOutsideBounds);
            if (inReach)
            {
                for (int i = 0; i < numItems; i++)
                {
                    double s = getAngleFor(i - 0.5, numItems);
                    double e = getAngleFor(i + 0.5, numItems);
                    if (a >= s && a < e)
                    {
                        hovered = i;
                        break;
                    }
                }
            }
        }
        setHovered(hovered);

        if (ClientConfig.clipMouseToCircle)
            clipMouse(owner);
    }

    private void clipMouse(Screen owner)
    {
        Window window = minecraft.getWindow();
        double scale = window.getScreenWidth() / (double) owner.width;
        double centerX = window.getScreenWidth() / 2.0;
        double centerY = window.getScreenHeight() / 2.0;
        double dx = minecraft.mouseHandler.xpos() - centerX;
        double dy = minecraft.mouseHandler.ypos() - centerY;

        double distance = Math.sqrt(dx * dx + dy * dy);
        double radius = radiusOut * scale * 0.975;
        if (distance > radius)
        {
            double guiX = (centerX + dx * radius / distance) / scale;
            double guiY = (centerY + dy * radius / distance) / scale;
            InputHelper.setGuiMousePosition(guiX, guiY);
        }
    }

    /** Angle of item {@code i} (fractional values give wedge edges). */
    private static double getAngleFor(double i, int numItems)
    {
        if (numItems == 0)
            return 0;
        return (i / numItems) * TWO_PI + START_ANGLE;
    }

    /** A ring segment drawn as a strip of quads. */
    public record PieArc(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float x,
            float y,
            float radiusIn,
            float radiusOut,
            float startAngle,
            float endAngle,
            int color,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState
    {
        public PieArc(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                      float x, float y, float radiusIn, float radiusOut, float startAngle, float endAngle, int color)
        {
            // The full outer circle's box is a safe (if loose) bound for any segment.
            this(pipeline, textureSetup, pose, x, y, radiusIn, radiusOut, startAngle, endAngle, color,
                    new ScreenRectangle(Mth.floor(x - radiusOut), Mth.floor(y - radiusOut),
                            Mth.ceil(radiusOut * 2) + 1, Mth.ceil(radiusOut * 2) + 1).transformMaxBounds(pose));
        }

        @Override
        public @Nullable ScreenRectangle scissorArea()
        {
            return null;
        }

        @Override
        public void buildVertices(VertexConsumer consumer)
        {
            float angle = endAngle - startAngle;
            int sections = Math.max(1, Mth.ceil(angle / PRECISION));
            float slice = angle / sections;

            for (int i = 0; i < sections; i++)
            {
                float angle1 = startAngle + i * slice;
                float angle2 = startAngle + (i + 1) * slice;

                float pos1InX = x + radiusIn * Mth.cos(angle1);
                float pos1InY = y + radiusIn * Mth.sin(angle1);
                float pos1OutX = x + radiusOut * Mth.cos(angle1);
                float pos1OutY = y + radiusOut * Mth.sin(angle1);
                float pos2OutX = x + radiusOut * Mth.cos(angle2);
                float pos2OutY = y + radiusOut * Mth.sin(angle2);
                float pos2InX = x + radiusIn * Mth.cos(angle2);
                float pos2InY = y + radiusIn * Mth.sin(angle2);

                consumer.addVertexWith2DPose(pose, pos1OutX, pos1OutY).setColor(color);
                consumer.addVertexWith2DPose(pose, pos1InX, pos1InY).setColor(color);
                consumer.addVertexWith2DPose(pose, pos2InX, pos2InY).setColor(color);
                consumer.addVertexWith2DPose(pose, pos2OutX, pos2OutY).setColor(color);
            }
        }
    }
}
