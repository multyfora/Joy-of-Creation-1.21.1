package net.multyfora.client.portable_typewriter;

import java.util.Map;

public final class TypewriterUvLayout {
    public static final int TEMPLATE_WIDTH = 256;
    public static final int TEMPLATE_HEIGHT = 512;

    public record Rect(float u0, float v0, float u1, float v1, boolean rotated) {}

    public record BoxUv(Rect top, Rect bottom, Rect front, Rect back, Rect left, Rect right) {}

    public static final BoxUv CASE = new BoxUv(
        new Rect(0.000000F, 0.000000F, 1.000000F, 0.222656F, false),
        new Rect(0.000000F, 0.226562F, 1.000000F, 0.449219F, false),
        new Rect(0.000000F, 0.544922F, 1.000000F, 0.566406F, false),
        new Rect(0.000000F, 0.570312F, 1.000000F, 0.591797F, false),
        new Rect(0.000000F, 0.595703F, 0.445312F, 0.617188F, false),
        new Rect(0.453125F, 0.595703F, 0.898438F, 0.617188F, false)
    );

    public static final BoxUv RIM_LEFT = new BoxUv(
        new Rect(0.000000F, 0.714844F, 0.445312F, 0.726562F, true),
        null,
        new Rect(0.687500F, 0.814453F, 0.710938F, 0.822266F, false),
        new Rect(0.718750F, 0.814453F, 0.742188F, 0.822266F, false),
        new Rect(0.000000F, 0.791016F, 0.445312F, 0.798828F, false),
        new Rect(0.453125F, 0.791016F, 0.898438F, 0.798828F, false)
    );

    public static final BoxUv RIM_RIGHT = new BoxUv(
        new Rect(0.453125F, 0.714844F, 0.898438F, 0.726562F, true),
        null,
        new Rect(0.750000F, 0.814453F, 0.773438F, 0.822266F, false),
        new Rect(0.781250F, 0.814453F, 0.804688F, 0.822266F, false),
        new Rect(0.000000F, 0.802734F, 0.445312F, 0.810547F, false),
        new Rect(0.453125F, 0.802734F, 0.898438F, 0.810547F, false)
    );

    public static final BoxUv RIM_FRONT = new BoxUv(
        new Rect(0.000000F, 0.699219F, 0.957031F, 0.710938F, false),
        null,
        new Rect(0.000000F, 0.744141F, 0.957031F, 0.751953F, false),
        new Rect(0.000000F, 0.755859F, 0.957031F, 0.763672F, false),
        null,
        null
    );

    public static final BoxUv RIM_BACK = new BoxUv(
        new Rect(0.000000F, 0.453125F, 0.957031F, 0.486328F, false),
        null,
        new Rect(0.000000F, 0.767578F, 0.957031F, 0.775391F, false),
        new Rect(0.000000F, 0.779297F, 0.957031F, 0.787109F, false),
        null,
        null
    );

    public static final BoxUv KEY_1U = new BoxUv(
        new Rect(0.085938F, 0.517578F, 0.132812F, 0.541016F, false),
        null,
        new Rect(0.687500F, 0.660156F, 0.742188F, 0.675781F, false),
        new Rect(0.750000F, 0.660156F, 0.804688F, 0.675781F, false),
        new Rect(0.812500F, 0.660156F, 0.867188F, 0.675781F, false),
        new Rect(0.875000F, 0.660156F, 0.929688F, 0.675781F, false)
    );

    public static final BoxUv KEY_1_5U = new BoxUv(
        new Rect(0.000000F, 0.517578F, 0.078125F, 0.541016F, false),
        null,
        new Rect(0.500000F, 0.660156F, 0.585938F, 0.675781F, false),
        new Rect(0.593750F, 0.660156F, 0.679688F, 0.675781F, false),
        new Rect(0.937500F, 0.660156F, 0.992188F, 0.675781F, false),
        new Rect(0.000000F, 0.679688F, 0.054688F, 0.695312F, false)
    );

    public static final BoxUv KEY_1_75U = new BoxUv(
        new Rect(0.851562F, 0.490234F, 0.945312F, 0.513672F, false),
        null,
        new Rect(0.281250F, 0.660156F, 0.382812F, 0.675781F, false),
        new Rect(0.390625F, 0.660156F, 0.492188F, 0.675781F, false),
        new Rect(0.062500F, 0.679688F, 0.117188F, 0.695312F, false),
        new Rect(0.125000F, 0.679688F, 0.179688F, 0.695312F, false)
    );

    public static final BoxUv KEY_2_25U = new BoxUv(
        new Rect(0.718750F, 0.490234F, 0.843750F, 0.513672F, false),
        null,
        new Rect(0.000000F, 0.660156F, 0.132812F, 0.675781F, false),
        new Rect(0.140625F, 0.660156F, 0.273438F, 0.675781F, false),
        new Rect(0.187500F, 0.679688F, 0.242188F, 0.695312F, false),
        new Rect(0.250000F, 0.679688F, 0.304688F, 0.695312F, false)
    );

    public static final BoxUv KEY_2_75U = new BoxUv(
        new Rect(0.554688F, 0.490234F, 0.710938F, 0.513672F, false),
        null,
        new Rect(0.562500F, 0.640625F, 0.726562F, 0.656250F, false),
        new Rect(0.734375F, 0.640625F, 0.898438F, 0.656250F, false),
        new Rect(0.312500F, 0.679688F, 0.367188F, 0.695312F, false),
        new Rect(0.375000F, 0.679688F, 0.429688F, 0.695312F, false)
    );

    public static final BoxUv KEY_9U = new BoxUv(
        new Rect(0.000000F, 0.490234F, 0.546875F, 0.513672F, false),
        null,
        new Rect(0.000000F, 0.621094F, 0.554688F, 0.636719F, false),
        new Rect(0.000000F, 0.640625F, 0.554688F, 0.656250F, false),
        new Rect(0.437500F, 0.679688F, 0.492188F, 0.695312F, false),
        new Rect(0.500000F, 0.679688F, 0.554688F, 0.695312F, false)
    );

    public static final BoxUv LED = new BoxUv(
        new Rect(0.906250F, 0.714844F, 0.968750F, 0.726562F, false),
        null,
        new Rect(0.812500F, 0.814453F, 0.875000F, 0.820312F, false),
        new Rect(0.882812F, 0.814453F, 0.945312F, 0.820312F, false),
        new Rect(0.953125F, 0.814453F, 0.976562F, 0.820312F, false),
        new Rect(0.000000F, 0.826172F, 0.023438F, 0.832031F, false)
    );

    public static final BoxUv ANTENNA_BASE = new BoxUv(
        new Rect(0.906250F, 0.595703F, 0.949219F, 0.617188F, false),
        null,
        new Rect(0.000000F, 0.730469F, 0.042969F, 0.740234F, false),
        new Rect(0.050781F, 0.730469F, 0.093750F, 0.740234F, false),
        new Rect(0.101562F, 0.730469F, 0.144531F, 0.740234F, false),
        new Rect(0.152344F, 0.730469F, 0.195312F, 0.740234F, false)
    );

    public static final BoxUv ANTENNA_ROD = new BoxUv(
        null,
        null,
        new Rect(0.000000F, 0.814453F, 0.164062F, 0.822266F, true),
        new Rect(0.171875F, 0.814453F, 0.335938F, 0.822266F, true),
        new Rect(0.343750F, 0.814453F, 0.507812F, 0.822266F, true),
        new Rect(0.515625F, 0.814453F, 0.679688F, 0.822266F, true)
    );

    public static final BoxUv ANTENNA_TIP = new BoxUv(
        new Rect(0.562500F, 0.679688F, 0.593750F, 0.695312F, false),
        new Rect(0.601562F, 0.679688F, 0.632812F, 0.695312F, false),
        new Rect(0.640625F, 0.679688F, 0.671875F, 0.695312F, false),
        new Rect(0.679688F, 0.679688F, 0.710938F, 0.695312F, false),
        new Rect(0.718750F, 0.679688F, 0.750000F, 0.695312F, false),
        new Rect(0.757812F, 0.679688F, 0.789062F, 0.695312F, false)
    );

    public static final Map<Float, BoxUv> KEYS = Map.of(
        1F, KEY_1U,
        1.5F, KEY_1_5U,
        1.75F, KEY_1_75U,
        2.25F, KEY_2_25U,
        2.75F, KEY_2_75U,
        9F, KEY_9U
    );

    private TypewriterUvLayout() {}
}
