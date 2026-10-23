package dev.gigaherz.toolbelt.client.radial;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class RadialMenuItem
{
    private static final float HOVER_TRANSITION_TIME = (1.0f / 0.2f) / 20.0f;

    private final GenericRadialMenu owner;
    private @Nullable Component centralText;
    private boolean visible;
    private boolean hovered;
    private float hoverState = 0;

    protected RadialMenuItem(GenericRadialMenu owner)
    {
        this.owner = owner;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean newVisible)
    {
        visible = newVisible;
        owner.visibilityChanged();
    }

    public @Nullable Component getCentralText()
    {
        return centralText;
    }

    public void setCentralText(@Nullable Component centralText)
    {
        this.centralText = centralText;
    }

    public boolean isHovered()
    {
        return hovered;
    }

    public void setHovered(boolean hovered)
    {
        this.hovered = hovered;
    }

    public abstract void extractRenderState(DrawingContext context);

    public abstract void prepareTooltip(DrawingContext context);

    /** Called when this item is chosen. Returns true if the click was handled. */
    public boolean onClick()
    {
        return false;
    }

    public float getHoverState(float partialTick)
    {
        if (hovered)
            return Math.min(hoverState + HOVER_TRANSITION_TIME * partialTick, 1.0f);
        else
            return Math.max(hoverState - HOVER_TRANSITION_TIME * partialTick, 0.0f);
    }

    public void tick()
    {
        if (hovered)
            hoverState = Math.min(hoverState + HOVER_TRANSITION_TIME, 1.0f);
        else
            hoverState = Math.max(hoverState - HOVER_TRANSITION_TIME, 0.0f);
    }
}
