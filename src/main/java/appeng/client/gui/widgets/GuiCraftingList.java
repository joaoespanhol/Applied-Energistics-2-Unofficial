package appeng.client.gui.widgets;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.core.AELog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

public class GuiCraftingList {
	
	private static final int FIELD_WIDTH = 69;
	private static final int FIELD_HEIGHT = 24;
	private static final String FIELD_TEXTURE = "guis/onefiled.png";
	
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
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
			try {
				fb.bindFramebuffer(true);

				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						GL11.glPushMatrix();
						parent.bindTexture(FIELD_TEXTURE);
						parent.drawTexturedModalRect(0, 0, 0, 0, FIELD_WIDTH,
								FIELD_HEIGHT);
						GL11.glPopMatrix();
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
							
							outputImg.setRGB(x * (FIELD_WIDTH - 1) + x_, y * (FIELD_HEIGHT - 1) + y_, downloadBuffer.get(FIELD_WIDTH * FIELD_HEIGHT - i - 1));
						}
						
						
					}
				}
                
//                for(int i = 0; i < imgWidth * imgHeight; i ++) {
//                	int x = i % imgWidth;
//                	int y = (int)(i / imgWidth);
//                }
                
//                for(int w = 0; w < imgWidth; w ++) {
//                	for(int h = 0; h < imgHeight; h ++) {
//                		outputImg.setRGB(w, h, downloadBuffer.get(imgHeight * h + w));
//                	}
//                }
                
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

}
