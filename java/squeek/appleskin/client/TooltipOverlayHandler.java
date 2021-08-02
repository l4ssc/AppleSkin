package squeek.appleskin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.AppleSkin;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.helpers.AppleCoreHelper;
import squeek.appleskin.helpers.BetterWithModsHelper;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;

@SideOnly(Side.CLIENT)
public class TooltipOverlayHandler
{
	private static ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID_LOWER, "textures/icons.png");
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -3;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new TooltipOverlayHandler());
	}

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.PostText event)
	{
		ItemStack hoveredStack = event.getStack();
		if (hoveredStack == null || hoveredStack.isEmpty())
			return;

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP;
		if (!shouldShowTooltip)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		GuiScreen gui = mc.currentScreen;

		if (gui == null)
			return;

		if (!FoodHelper.isFood(hoveredStack))
			return;

		EntityPlayer player = mc.player;
		ScaledResolution scale = new ScaledResolution(mc);
		int toolTipY = event.getY();
		int toolTipX = event.getX();
		int toolTipW = event.getWidth();
		int toolTipH = event.getHeight();

		FoodHelper.BasicFoodValues defaultFoodValues = FoodHelper.getDefaultFoodValues(hoveredStack);
		FoodHelper.BasicFoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(hoveredStack, player);

		// Apply scale for altered max hunger if necessary
		if (AppleSkin.hasAppleCore)
		{
			defaultFoodValues = AppleCoreHelper.getFoodValuesForDisplay(defaultFoodValues, player);
			modifiedFoodValues = AppleCoreHelper.getFoodValuesForDisplay(modifiedFoodValues, player);
		}

		// Apply BWM tweaks if necessary
		defaultFoodValues = BetterWithModsHelper.getFoodValuesForDisplay(defaultFoodValues);
		modifiedFoodValues = BetterWithModsHelper.getFoodValuesForDisplay(modifiedFoodValues);

		if (defaultFoodValues.equals(modifiedFoodValues) && defaultFoodValues.hunger == 0)
			return;

		int biggestHunger = Math.max(defaultFoodValues.hunger, modifiedFoodValues.hunger);

		int barsNeeded = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
		boolean hungerOverflow = barsNeeded > 10;
		String hungerText = hungerOverflow ? ((biggestHunger < 0 ? -1 : 1) * barsNeeded) + "x " : null;
		if (hungerOverflow)
			barsNeeded = 1;

		int toolTipBottomY = toolTipY + toolTipH + 1 + TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM;
		int toolTipRightX = toolTipX + toolTipW + 1 + TOOLTIP_REAL_WIDTH_OFFSET_RIGHT;

		boolean shouldDrawBelow = toolTipBottomY + 20 < scale.getScaledHeight() - 3;

		int rightX = toolTipRightX - 3;
		int leftX = rightX - barsNeeded * 9 + (int) (mc.fontRenderer.getStringWidth(hungerText) * 0.75f - 3);
		int topY = (shouldDrawBelow ? toolTipBottomY : toolTipY - 20 + TOOLTIP_REAL_HEIGHT_OFFSET_TOP);
		int bottomY = topY + 11;

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();

		// bg
		Gui.drawRect(leftX - 2, topY, rightX + 1, bottomY, 0xF0100010);
		Gui.drawRect(leftX - 1, (shouldDrawBelow ? bottomY : topY - 1), rightX, (shouldDrawBelow ? bottomY + 1 : topY), 0xF0100010);
		Gui.drawRect(leftX - 1, topY, rightX, bottomY, 0x66FFFFFF);

		// drawRect disables blending and modifies color, so reset them
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		int x = rightX - 2;
		int y = bottomY - 10;

		mc.getTextureManager().bindTexture(modIcons);

		for (int i = 0; i < barsNeeded * 2; i += 2)
		{
			x -= 9;

			gui.drawTexturedModalRect(x, y, 36, 0, 9, 9);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodValues.hunger > i)
				gui.drawTexturedModalRect(x, y, modifiedFoodValues.hunger - 1 == i ? 54 : 45, 0, 9, 9);
		}
		if (hungerText != null)
		{
			GlStateManager.pushMatrix();
			GlStateManager.scale(0.75F, 0.75F, 0.75F);
			mc.fontRenderer.drawStringWithShadow(hungerText, x * 4 / 3 - mc.fontRenderer.getStringWidth(hungerText) + 2, y * 4 / 3 + 2, 0xFFDDDDDD);
			GlStateManager.popMatrix();
		}

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
	}
}
