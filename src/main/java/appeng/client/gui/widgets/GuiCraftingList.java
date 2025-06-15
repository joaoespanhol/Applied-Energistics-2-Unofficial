package appeng.client.gui.widgets;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.base.Joiner;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.util.ColorPickHelper;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.RoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

public class GuiCraftingList {
	
	private static final int FIELD_WIDTH = 69;
	private static final int FIELD_HEIGHT = 24;
	private static final int FIELD_SECTIONLENGTH = 67;
	private static final ResourceLocation FIELD_TEXTURE = new ResourceLocation(AppEng.MOD_ID, "textures/guis/onefiled.png");
	
    private static final DateTimeFormatter SCREENSHOT_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
	
	public static void saveScreenShot(AEBaseGui parent, List<IAEItemStack> visual, IItemList<IAEItemStack> storage,
			IItemList<IAEItemStack> pending, IItemList<IAEItemStack> missing) {
		// Make a better size for reading
		int visualSize = visual.size();
		int width = 3;
		int height = 0;
		while(true) {
			height = (int)((visualSize  * 1.0) / width + 1);
			// Make sure aspect ratio is under 0.9, like a square
			if((width * 68 + 1) * 1.0 / (height * 23 + 1) >= 0.9) {
				break;
			}
			width ++;
		}
		
        final Minecraft mc = Minecraft.getMinecraft();
        if (!OpenGlHelper.isFramebufferEnabled()) {
            AELog.error("Could not save crafting tree screenshot: FBOs disabled/unsupported");
            mc.ingameGUI.getChatGUI()
                    .printChatMessage(new ChatComponentTranslation("chat.appliedenergistics2.FBOUnsupported"));
            return;
        }
        
        try {
        	
            final File screenshotsDir = new File(mc.mcDataDir, "screenshots");
            FileUtils.forceMkdir(screenshotsDir);
            
            int imgWidth = width * (FIELD_WIDTH - 1) + 1;
            int imgHeight = height * (FIELD_HEIGHT - 1) + 1;

            final BufferedImage outputImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
            final IntBuffer downloadBuffer = BufferUtils.createIntBuffer(FIELD_WIDTH * FIELD_HEIGHT);
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            final Framebuffer fb = new Framebuffer(FIELD_WIDTH, FIELD_HEIGHT, true);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, FIELD_WIDTH, FIELD_HEIGHT, 0, -3000, 3000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
			try {
				fb.bindFramebuffer(true);

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						if(width * y + x < visualSize) {
							GL11.glPushMatrix();

							GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
							parent.bindTexture(FIELD_TEXTURE);
							parent.drawTexturedModalRect(0, 0, 0, 0, FIELD_WIDTH,
									FIELD_HEIGHT);
							GL11.glPopMatrix();
							test(parent, visual.get(width * y + x), visual, storage, pending, missing);
							AELog.info("%d, %s", width * y + x, visual.get(width * y + x).getItemStack().getDisplayName());
							
							
							GL11.glBindTexture(GL11.GL_TEXTURE_2D, fb.framebufferTexture);
							GL11.glGetTexImage(
									GL11.GL_TEXTURE_2D,
									0,
									GL12.GL_BGRA,
									GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
									downloadBuffer);
							
							for(int i = 0; i < FIELD_WIDTH * FIELD_HEIGHT; i ++) {
								int x_ = i % FIELD_WIDTH;
								int y_ = (int)(i / FIELD_WIDTH);
								outputImg.setRGB(x * (FIELD_WIDTH - 1) + x_, (y + 1) * (FIELD_HEIGHT - 1) - y_, downloadBuffer.get(i));
							}
						}
						
					}
				}
                
			} finally {
				fb.deleteFramebuffer();
				GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
			}
			GL11.glPopAttrib();
			GL11.glPopMatrix();
            
            final String date = SCREENSHOT_DATE_FORMAT.format(LocalDateTime.now());
            String filename = String.format("%s-ae2.png", date);
            File outFile = new File(screenshotsDir, filename);
            for (int i = 1; outFile.exists() && i < 99; i++) {
                filename = String.format("%s-ae2-%d.png", date, i);
                outFile = new File(screenshotsDir, filename);
            }
            if (outFile.exists()) {
                throw new FileAlreadyExistsException(filename);
            }
            ImageIO.write(outputImg, "png", outFile);

            AELog.info("Saved crafting list screenshot to %s", filename);
            ChatComponentText chatLink = new ChatComponentText(filename);
            chatLink.getChatStyle()
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFile.getAbsolutePath()));
            chatLink.getChatStyle().setUnderlined(Boolean.valueOf(true));
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentTranslation("screenshot.success", chatLink));
        	
        } catch (Exception e) {
            AELog.warn(e, "Could not save crafting list screenshot");
            mc.ingameGUI.getChatGUI()
                    .printChatMessage(new ChatComponentTranslation("screenshot.failure", e.getMessage()));
        }
	}
	
	private static void test(AEBaseGui parent, IAEItemStack refStack, List<IAEItemStack> visual, IItemList<IAEItemStack> storage,
			IItemList<IAEItemStack> pending, IItemList<IAEItemStack> missing) {
        final int xo = 9;
        final int yo = 22;
		if (refStack != null) {
            GL11.glPushMatrix();
//            GL11.glScaled(0.5, 0.5, 0.5);
////
//            final IAEItemStack stored = storage.findPrecise(refStack);
//            final IAEItemStack pendingStack = pending.findPrecise(refStack);
//            final IAEItemStack missingStack = missing.findPrecise(refStack);
////
//            int lines = 0;
//
//            if (stored != null && stored.getStackSize() > 0) {
//                lines++;
//                if (missingStack == null && pendingStack == null) {
//                    lines++;
//                }
//            }
//            if (missingStack != null && missingStack.getStackSize() > 0) {
//                lines++;
//            }
//            if (pendingStack != null && pendingStack.getStackSize() > 0) {
//                lines += 2;
//            }
//
//            final int negY = ((lines - 1) * 5) / 2;
//            int downY = 0;
//
//            if (stored != null && stored.getStackSize() > 0) {
//                String str = GuiText.FromStorage.getLocal() + ": "
//                        + ReadableNumberConverter.INSTANCE.toWideReadableForm(stored.getStackSize());
//                final int w = 4 + parent.getFontRenderer().getStringWidth(str);
//                parent.getFontRenderer().drawString(
//                        str,
////                        (int) ((xo + FIELD_SECTIONLENGTH - 19 - (w * 0.5)) * 2),
//                        9,
//                        (yo + 6 - negY + downY) * 2,
//                        GuiColors.CraftConfirmFromStorage.getColor());
////
//                downY += 5;
//            }
//
//            if (missingStack != null && missingStack.getStackSize() > 0) {
//                String str = GuiText.Missing.getLocal() + ": "
//                        + ReadableNumberConverter.INSTANCE.toWideReadableForm(missingStack.getStackSize());
//                final int w = 4 + parent.getFontRenderer().getStringWidth(str);
//                parent.getFontRenderer().drawString(
//                        str,
//                        (int) ((xo + FIELD_SECTIONLENGTH - 19 - (w * 0.5)) * 2),
//                        (yo + 6 - negY + downY) * 2,
//                        GuiColors.CraftConfirmMissing.getColor());
//
//                downY += 5;
//            }
//
//            if (pendingStack != null && pendingStack.getStackSize() > 0) {
//                String str = GuiText.ToCraft.getLocal() + ": "
//                        + ReadableNumberConverter.INSTANCE.toWideReadableForm(pendingStack.getStackSize());
//                int w = 4 + parent.getFontRenderer().getStringWidth(str);
//                parent.getFontRenderer().drawString(
//                        str,
//                        (int) ((xo + FIELD_SECTIONLENGTH - 19 - (w * 0.5)) * 2),
//                        (yo + 6 - negY + downY) * 2,
//                        GuiColors.CraftConfirmToCraft.getColor());
//
//                downY += 5;
//                str = GuiText.ToCraftRequests.getLocal() + ": "
//                        + ReadableNumberConverter.INSTANCE
//                                .toWideReadableForm(pendingStack.getCountRequestableCrafts());
//                w = 4 + parent.getFontRenderer().getStringWidth(str);
//                parent.getFontRenderer().drawString(
//                        str,
//                        (int) ((xo + FIELD_SECTIONLENGTH - 19 - (w * 0.5)) * 2),
//                        (yo + 6 - negY + downY) * 2,
//                        GuiColors.CraftConfirmToCraft.getColor());
//            }
//
//            if (stored != null && stored.getStackSize() > 0 && missingStack == null && pendingStack == null) {
//                String str = GuiText.FromStoragePercent.getLocal() + ": "
//                        + RoundHelper.toRoundedFormattedForm(stored.getUsedPercent(), 2)
//                        + "%";
//                int w = 4 + parent.getFontRenderer().getStringWidth(str);
//                parent.getFontRenderer().drawString(
//                        str,
//                        (int) ((xo + FIELD_SECTIONLENGTH - 19 - (w * 0.5)) * 2),
//                        (yo + 6 - negY + downY) * 2,
//                        ColorPickHelper.selectColorFromThreshold(stored.getUsedPercent()).getColor());
//            }

            GL11.glPopMatrix();
            final int posX = FIELD_SECTIONLENGTH - 18;

            final ItemStack is = refStack.copy().getItemStack();
            GL11.glPushMatrix();
            parent.drawItem(posX, FIELD_HEIGHT / 2 - 8, is);
            GL11.glPopMatrix();

//            if (red) {
//                final int startX = xo;
//                final int startY = posY - 4;
//                Gui.drawRect(
//                        startX,
//                        startY,
//                        startX + FIELD_SECTIONLENGTH,
//                        startY + offY,
//                        GuiColors.CraftConfirmMissingItem.getColor());
//            }
        }
	}

}
