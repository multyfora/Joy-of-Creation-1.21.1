package net.multyfora.client.portable_typewriter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.multyfora.AeronauticsJoyofcreation;
import org.joml.Vector3f;

import java.util.Set;

public final class TypewriterKeyboardModel {
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(AeronauticsJoyofcreation.MODID, "textures/item/portable_typewriter.png");

    private static final float UNIT = 1.0F;
    private static final float KEY_GAP = 0.1F;
    private static final float KEY_DEPTH = 0.9F;
    private static final float ROW_PITCH = 1.1F;
    private static final int ROWS = TypewriterLayout.ROWS.size();

    private static final float AREA_WIDTH = TypewriterLayout.ROW_UNITS * UNIT - KEY_GAP;
    private static final float AREA_DEPTH = (ROWS - 1) * ROW_PITCH + KEY_DEPTH;

    private static final float SIDE_MARGIN = 0.55F;
    private static final float FRONT_MARGIN = 0.55F;
    private static final float BACK_MARGIN = 1.25F;
    private static final float DECK_PADDING = 0.2F;

    public static final float WIDTH = AREA_WIDTH + 2 * SIDE_MARGIN;
    public static final float DEPTH = AREA_DEPTH + FRONT_MARGIN + BACK_MARGIN;
    public static final float CASE_HEIGHT = 0.7F;
    private static final float RIM_HEIGHT = 0.25F;

    private static final float KEY_HEIGHT = 0.5F;
    private static final float KEY_TRAVEL = 0.3F;
    private static final float KEY_TOP_INSET = 0.08F;
    public static final float HEIGHT = CASE_HEIGHT + KEY_HEIGHT;

    private static final float LEGEND_SCALE = 0.045F;
    private static final float LEGEND_MARGIN = 0.12F;

    private static final int WHITE = 0xFFFFFF;
    private static final int KEY_BOUND = 0x5FD86E;
    private static final int KEY_BOUND_PRESSED = 0xB8FFC0;
    private static final int KEY_PRESSED = 0xC8C8C8;
    private static final int LEGEND_UNBOUND = 0x3A2A1C;
    private static final int LEGEND_BOUND = 0x16361A;

    private static final int LED_ACTIVE = 0x55FF66;
    private static final int LED_BIND = 0xFF9933;
    private static final int LED_OFF = 0x223322;
    private static final int ANTENNA_TIP_ON = 0xFF3A3A;
    private static final int ANTENNA_TIP_OFF = 0x6A1A1A;

    private TypewriterKeyboardModel() {}

    public static void render(PoseStack ms, MultiBufferSource buffers, int light, int overlay,
                              Set<Integer> boundKeys, boolean live, boolean legends) {
        PortableTypewriterClientHandler.Mode mode = live ? PortableTypewriterClientHandler.MODE : PortableTypewriterClientHandler.Mode.IDLE;
        boolean transmitting = false;

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        Mesh mesh = new Mesh(ms.last(), vc, overlay);

        renderCase(mesh, light);

        for (TypewriterLayout.PlacedKey placed : TypewriterLayout.PLACED_KEYS) {
            int code = placed.key().glfwCode();
            boolean bound = boundKeys.contains(code);
            float press = live ? TypewriterVisualState.getPress(code) : 0.0F;
            if (bound && press > 0.0F && mode == PortableTypewriterClientHandler.Mode.ACTIVE) {
                transmitting = true;
            }
            renderKey(mesh, placed, bound, press, keyLight(light, bound, press, mode));
        }

        renderStatusLed(mesh, mode, light);
        renderAntenna(ms, vc, overlay, light, transmitting);

        if (legends) {
            Font font = Minecraft.getInstance().font;
            for (TypewriterLayout.PlacedKey placed : TypewriterLayout.PLACED_KEYS) {
                int code = placed.key().glfwCode();
                boolean bound = boundKeys.contains(code);
                float press = live ? TypewriterVisualState.getPress(code) : 0.0F;
                renderLegend(ms, buffers, font, placed, bound, press, keyLight(light, bound, press, mode));
            }
        }
    }

    private static void renderCase(Mesh mesh, int light) {
        float x0 = -WIDTH / 2, x1 = WIDTH / 2;
        float z0 = -DEPTH / 2, z1 = DEPTH / 2;
        float y1 = CASE_HEIGHT;

        mesh.box(x0, 0, z0, x1, y1, z1, TypewriterUvLayout.CASE, WHITE, light);

        float deckX0 = keyAreaX0() - DECK_PADDING;
        float deckX1 = -deckX0;
        float deckZ0 = keyAreaZ0() - DECK_PADDING;
        float deckZ1 = keyAreaZ0() + AREA_DEPTH + DECK_PADDING;
        float rimTop = y1 + RIM_HEIGHT;

        mesh.box(x0, y1, z0, deckX0, rimTop, z1, TypewriterUvLayout.RIM_LEFT, WHITE, light);
        mesh.box(deckX1, y1, z0, x1, rimTop, z1, TypewriterUvLayout.RIM_RIGHT, WHITE, light);
        mesh.box(deckX0, y1, deckZ1, deckX1, rimTop, z1, TypewriterUvLayout.RIM_FRONT, WHITE, light);
        mesh.box(deckX0, y1, z0, deckX1, rimTop, deckZ0, TypewriterUvLayout.RIM_BACK, WHITE, light);
    }

    private static void renderKey(Mesh mesh, TypewriterLayout.PlacedKey placed, boolean bound, float press, int light) {
        float x0 = keyX0(placed);
        float x1 = x0 + placed.key().widthUnits() * UNIT - KEY_GAP;
        float z0 = keyAreaZ0() + placed.row() * ROW_PITCH;
        float z1 = z0 + KEY_DEPTH;
        float sink = press * KEY_TRAVEL;

        TypewriterUvLayout.BoxUv uv = TypewriterUvLayout.KEYS.getOrDefault(placed.key().widthUnits(), TypewriterUvLayout.KEY_1U);
        mesh.tapered(x0, z0, x1, z1, CASE_HEIGHT - sink, CASE_HEIGHT + KEY_HEIGHT - sink, KEY_TOP_INSET,
                uv, keyTint(bound, press), light);
    }

    private static void renderLegend(PoseStack ms, MultiBufferSource buffers, Font font,
                                     TypewriterLayout.PlacedKey placed, boolean bound, float press, int light) {
        String label = placed.key().label();
        float x0 = keyX0(placed);
        float topWidth = placed.key().widthUnits() * UNIT - KEY_GAP - 2 * KEY_TOP_INSET;
        float cx = x0 + (placed.key().widthUnits() * UNIT - KEY_GAP) / 2;
        float cz = keyAreaZ0() + placed.row() * ROW_PITCH + KEY_DEPTH / 2;
        float y = CASE_HEIGHT + KEY_HEIGHT - press * KEY_TRAVEL + 0.005F;

        int textWidth = font.width(label);
        float scale = Math.min(LEGEND_SCALE, (topWidth - LEGEND_MARGIN) / textWidth);
        int color = 0xFF000000 | (bound || press > 0.5F ? LEGEND_BOUND : LEGEND_UNBOUND);

        ms.pushPose();
        ms.translate(cx, y, cz);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0F));
        ms.scale(scale, -scale, scale);
        font.drawInBatch(label, -textWidth / 2.0F, -3.5F, color, false, ms.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        ms.popPose();
    }

    private static void renderStatusLed(Mesh mesh, PortableTypewriterClientHandler.Mode mode, int light) {
        float rimTop = CASE_HEIGHT + RIM_HEIGHT;
        float cz = -DEPTH / 2 + (BACK_MARGIN - DECK_PADDING) / 2;
        int color = switch (mode) {
            case ACTIVE -> LED_ACTIVE;
            case BIND -> LED_BIND;
            case IDLE -> LED_OFF;
        };
        int ledLight = mode == PortableTypewriterClientHandler.Mode.IDLE ? light : LightTexture.FULL_BRIGHT;
        mesh.tapered(5.6F, cz - 0.2F, 6.6F, cz + 0.2F, rimTop - 0.05F, rimTop + 0.12F, 0.05F,
                TypewriterUvLayout.LED, color, ledLight);
    }

    private static void renderAntenna(PoseStack ms, VertexConsumer vc, int overlay, int light, boolean transmitting) {
        float rimTop = CASE_HEIGHT + RIM_HEIGHT;
        float cz = -DEPTH / 2 + (BACK_MARGIN - DECK_PADDING) / 2;
        float cx = -6.6F;

        ms.pushPose();
        ms.translate(cx, rimTop, cz);
        Mesh mesh = new Mesh(ms.last(), vc, overlay);
        mesh.box(-0.35F, 0, -0.35F, 0.35F, 0.3F, 0.35F, TypewriterUvLayout.ANTENNA_BASE, WHITE, light);

        ms.translate(0, 0.3F, 0);
        ms.mulPose(Axis.XP.rotationDegrees(-55.0F));
        mesh = new Mesh(ms.last(), vc, overlay);
        mesh.box(-0.11F, -0.25F, -0.11F, 0.11F, 2.6F, 0.11F, TypewriterUvLayout.ANTENNA_ROD, WHITE, light);
        mesh.box(-0.25F, 2.6F, -0.25F, 0.25F, 3.1F, 0.25F, TypewriterUvLayout.ANTENNA_TIP,
                transmitting ? ANTENNA_TIP_ON : ANTENNA_TIP_OFF, transmitting ? LightTexture.FULL_BRIGHT : light);
        ms.popPose();
    }

    private static float keyAreaZ0() {
        return -DEPTH / 2 + BACK_MARGIN;
    }

    private static float keyAreaX0() {
        return -AREA_WIDTH / 2;
    }

    private static float keyX0(TypewriterLayout.PlacedKey placed) {
        return keyAreaX0() + placed.x() * UNIT;
    }

    private static int keyTint(boolean bound, float press) {
        return bound ? lerpColor(KEY_BOUND, KEY_BOUND_PRESSED, press) : lerpColor(WHITE, KEY_PRESSED, press);
    }

    private static int keyLight(int light, boolean bound, float press, PortableTypewriterClientHandler.Mode mode) {
        if (!bound || press <= 0.0F || mode != PortableTypewriterClientHandler.Mode.ACTIVE) return light;
        int block = Math.max(LightTexture.block(light), Math.round(15 * press));
        return LightTexture.pack(block, LightTexture.sky(light));
    }

    private static int lerpColor(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, FastColor.ARGB32.red(from), FastColor.ARGB32.red(to)));
        int g = Math.round(Mth.lerp(t, FastColor.ARGB32.green(from), FastColor.ARGB32.green(to)));
        int b = Math.round(Mth.lerp(t, FastColor.ARGB32.blue(from), FastColor.ARGB32.blue(to)));
        return (r << 16) | (g << 8) | b;
    }

    private record Mesh(PoseStack.Pose pose, VertexConsumer vc, int overlay) {

        void box(float x0, float y0, float z0, float x1, float y1, float z1,
                 TypewriterUvLayout.BoxUv uv, int tint, int light) {
            tapered(x0, z0, x1, z1, y0, y1, 0.0F, uv, tint, light);
        }

        void tapered(float x0, float z0, float x1, float z1, float yBottom, float yTop, float inset,
                     TypewriterUvLayout.BoxUv uv, int tint, int light) {
            float tx0 = x0 + inset, tx1 = x1 - inset, tz0 = z0 + inset, tz1 = z1 - inset;
            Vector3f center = new Vector3f((x0 + x1) / 2, (yBottom + yTop) / 2, (z0 + z1) / 2);

            quad(center, uv.top(), tint, light,
                    tx0, yTop, tz1, tx1, yTop, tz1, tx1, yTop, tz0, tx0, yTop, tz0);
            quad(center, uv.bottom(), tint, light,
                    x0, yBottom, z1, x1, yBottom, z1, x1, yBottom, z0, x0, yBottom, z0);
            quad(center, uv.front(), tint, light,
                    x0, yBottom, z1, x1, yBottom, z1, tx1, yTop, tz1, tx0, yTop, tz1);
            quad(center, uv.back(), tint, light,
                    x1, yBottom, z0, x0, yBottom, z0, tx0, yTop, tz0, tx1, yTop, tz0);
            quad(center, uv.right(), tint, light,
                    x1, yBottom, z1, x1, yBottom, z0, tx1, yTop, tz0, tx1, yTop, tz1);
            quad(center, uv.left(), tint, light,
                    x0, yBottom, z0, x0, yBottom, z1, tx0, yTop, tz1, tx0, yTop, tz0);
        }

        private void quad(Vector3f center, TypewriterUvLayout.Rect uv, int rgb, int light,
                          float ax, float ay, float az, float bx, float by, float bz,
                          float cx, float cy, float cz, float dx, float dy, float dz) {
            if (uv == null) return;
            Vector3f a = new Vector3f(ax, ay, az), b = new Vector3f(bx, by, bz);
            Vector3f c = new Vector3f(cx, cy, cz), d = new Vector3f(dx, dy, dz);
            Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a));
            if (normal.lengthSquared() < 1.0e-12F) {
                normal = new Vector3f(c).sub(b).cross(new Vector3f(a).sub(b));
            }
            normal.normalize();

            Vector3f faceCenter = new Vector3f(a).add(b).add(c).add(d).mul(0.25F);
            boolean flip = normal.dot(faceCenter.sub(center)) < 0;
            if (flip) normal.negate();

            float[] u = uv.rotated()
                ? new float[] {uv.u0(), uv.u0(), uv.u1(), uv.u1()}
                : new float[] {uv.u0(), uv.u1(), uv.u1(), uv.u0()};
            float[] v = uv.rotated()
                ? new float[] {uv.v0(), uv.v1(), uv.v1(), uv.v0()}
                : new float[] {uv.v1(), uv.v1(), uv.v0(), uv.v0()};
            int argb = 0xFF000000 | rgb;

            if (!flip) {
                vertex(a, u[0], v[0], argb, light, normal);
                vertex(b, u[1], v[1], argb, light, normal);
                vertex(c, u[2], v[2], argb, light, normal);
                vertex(d, u[3], v[3], argb, light, normal);
            } else {
                vertex(d, u[3], v[3], argb, light, normal);
                vertex(c, u[2], v[2], argb, light, normal);
                vertex(b, u[1], v[1], argb, light, normal);
                vertex(a, u[0], v[0], argb, light, normal);
            }
        }

        private void vertex(Vector3f p, float u, float v, int argb, int light, Vector3f n) {
            vc.addVertex(pose, p.x, p.y, p.z)
                    .setColor(argb)
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, n.x, n.y, n.z);
        }
    }
}
