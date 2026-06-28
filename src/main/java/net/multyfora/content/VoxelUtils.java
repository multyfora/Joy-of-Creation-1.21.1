package net.multyfora.content;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoxelUtils {
    public static VoxelShape combineVoxelShapes(VoxelShape... shapes) {
        if(shapes.length < 1) {
            return null;
        }

        VoxelShape combined = shapes[0];
        for(int index = 1; index < shapes.length; index++) {
            combined = Shapes.join(
                combined,
                shapes[index],
                BooleanOp.OR
            );
        }
        return combined;
    }

    public static VoxelShape rotateShape(Direction.Axis axis, VoxelShape shape) {
        if( axis.equals(Direction.Axis.Y) ) {
            return shape;
        }

        VoxelShape[] rotated = new VoxelShape[]{ Shapes.empty() };
        boolean isAxisX = axis.equals(Direction.Axis.X);
        shape.forAllBoxes(
                (minX, minY, minZ, maxX, maxY, maxZ) -> {
                    rotated[0] = Shapes.or(
                            rotated[0],
                            Shapes.create(
                                    isAxisX ? minY : minX,
                                    isAxisX ? minX : minZ,
                                    isAxisX ? minZ : minY,
                                    isAxisX ? maxY : maxX,
                                    isAxisX ? maxX : maxZ,
                                    isAxisX ? maxZ : maxY
                            )
                    );
                }
        );

        return rotated[0];
    }
}
