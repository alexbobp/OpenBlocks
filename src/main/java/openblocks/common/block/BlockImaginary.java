package openblocks.common.block;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import openblocks.OpenBlocks.Sounds;
import openblocks.common.tileentity.TileEntityImaginary;
import openblocks.common.tileentity.TileEntityImaginary.Property;
import openmods.block.OpenBlock;
import openmods.geometry.BlockSpaceTransform;
import openmods.geometry.Orientation;
import openmods.utils.BlockUtils;

public class BlockImaginary extends OpenBlock.FourDirections {

	public enum Type implements IStringSerializable {
		PENCIL, CRAYON;

		private final String name = name().toLowerCase();

		@Override
		public String getName() {
			return name;
		}
	}

	public enum Shape implements IStringSerializable {
		BLOCK {
			@Override
			public void addCollisions(Orientation orientation, BlockPos pos, AxisAlignedBB region, List<AxisAlignedBB> result) {
				AxisAlignedBB aabb = BlockUtils.singleBlock(pos);
				addBox(aabb, region, result);
			}

			@Override
			public AxisAlignedBB getBlockBounds(Orientation orientation) {
				return FULL_BLOCK_AABB;
			}
		},
		PANEL {
			@Override
			public void addCollisions(Orientation orientation, BlockPos pos, AxisAlignedBB region, List<AxisAlignedBB> result) {
				AxisAlignedBB aabb = FULL_PANEL_AABB.offset(pos);
				addBox(aabb, region, result);
			}

			@Override
			public AxisAlignedBB getBlockBounds(Orientation orientation) {
				return FULL_PANEL_AABB;
			}
		},
		HALF_PANEL {
			@Override
			public void addCollisions(Orientation orientation, BlockPos pos, AxisAlignedBB region, List<AxisAlignedBB> result) {
				AxisAlignedBB aabb = HALF_PANEL_AABB.offset(pos);
				addBox(aabb, region, result);
			}

			@Override
			public AxisAlignedBB getBlockBounds(Orientation orientation) {
				return HALF_PANEL_AABB;
			}

		},
		STAIRS {
			@Override
			public void addCollisions(Orientation orientation, BlockPos pos, AxisAlignedBB region, List<AxisAlignedBB> result) {
				AxisAlignedBB lowerStep = BlockSpaceTransform.instance.mapBlockToWorld(orientation, LOWER_STEP_PANEL_AABB).offset(pos);
				addBox(lowerStep, region, result);

				AxisAlignedBB upperStep = BlockSpaceTransform.instance.mapBlockToWorld(orientation, UPPER_STEP_PANEL_AABB).offset(pos);
				addBox(upperStep, region, result);
			}

			@Override
			public AxisAlignedBB getBlockBounds(Orientation orientation) {
				return BlockSpaceTransform.instance.mapBlockToWorld(orientation, STAIRS_AABB);
			}
		};

		public abstract void addCollisions(Orientation orientation, BlockPos pos, AxisAlignedBB region, List<AxisAlignedBB> result);

		public abstract AxisAlignedBB getBlockBounds(Orientation orientation);

		public final static Shape[] VALUES = values();

		private final String name = name().toLowerCase();

		@Override
		public String getName() {
			return name;
		}

		private static void addBox(AxisAlignedBB aabb, AxisAlignedBB region, List<AxisAlignedBB> result) {
			if (aabb.intersects(region)) result.add(aabb);
		}

	}

	public static final PropertyEnum<Shape> PROPERTY_SHAPE = PropertyEnum.create("shape", Shape.class);

	public static final PropertyEnum<Type> PROPERTY_TYPE = PropertyEnum.create("type", Type.class);

	private static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

	public static final double PANEL_HEIGHT = 0.1;

	private static final AxisAlignedBB FULL_PANEL_AABB = new AxisAlignedBB(0, 1 - PANEL_HEIGHT, 0, 1, 1, 1);

	private static final AxisAlignedBB HALF_PANEL_AABB = new AxisAlignedBB(0, 0.5 - PANEL_HEIGHT, 0, 1, 0.5, 1);

	private static final AxisAlignedBB STAIRS_AABB = new AxisAlignedBB(0, 0.5, 0, 1, 1, 1);

	private static final AxisAlignedBB LOWER_STEP_PANEL_AABB = new AxisAlignedBB(0, 0.5 - PANEL_HEIGHT, 0, 1, 0.5, 0.5);

	private static final AxisAlignedBB UPPER_STEP_PANEL_AABB = new AxisAlignedBB(0, 1 - PANEL_HEIGHT, 0.5, 1, 1, 1);

	private static final Material IMAGINARY = new Material(MapColor.AIR) {
		@Override
		public boolean blocksMovement() {
			return false;
		}

		@Override
		public boolean blocksLight() {
			return false;
		}
	};

	public BlockImaginary() {
		super(IMAGINARY);
		setDefaultState(getDefaultState().withProperty(PROPERTY_SHAPE, Shape.BLOCK).withProperty(PROPERTY_TYPE, Type.PENCIL));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { getPropertyOrientation(), PROPERTY_TYPE, PROPERTY_SHAPE });
	}

	public void setSoundType() {
		setSoundType(new SoundType(0.5f, 1.0f,
				Sounds.ITEM_CRAYON_PLACE,
				Sounds.ITEM_CRAYON_PLACE,
				Sounds.ITEM_CRAYON_PLACE,
				Sounds.ITEM_CRAYON_PLACE,
				Sounds.ITEM_CRAYON_PLACE));
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
		TileEntityImaginary te = getTileEntity(world, pos, TileEntityImaginary.class);
		if (te != null && te.is(Property.SELECTABLE)) {
			final Orientation orientation = state.getValue(getPropertyOrientation());
			return te.getShape().getBlockBounds(orientation).offset(pos);
		}

		return EMPTY_AABB;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return NULL_AABB;
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean something) {
		TileEntityImaginary te = getTileEntity(world, pos, TileEntityImaginary.class);
		if (te != null && te.is(Property.SOLID, entity)) {
			final Orientation orientation = state.getValue(getPropertyOrientation());
			te.getShape().addCollisions(orientation, pos, entityBox, collidingBoxes);
		}
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return EMPTY_AABB;
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState blockState, World world, BlockPos pos, Vec3d start, Vec3d end) {
		TileEntityImaginary te = getTileEntity(world, pos, TileEntityImaginary.class);
		if (world.isRemote) {
			if (te == null || !te.is(Property.SELECTABLE)) return null;
		}

		final AxisAlignedBB box = te.getShape().getBlockBounds(blockState.getValue(getPropertyOrientation()));
		return rayTrace(pos, start, end, box);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullBlock(IBlockState state) {
		return false;
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		return Lists.newArrayList();
	}

	@Override
	protected boolean suppressPickBlock() {
		return true;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		TileEntityImaginary te = getTileEntity(worldIn, pos, TileEntityImaginary.class);
		if (te != null) {
			final Shape shape = te.getShape();
			final Type type = te.getType();
			return state.withProperty(PROPERTY_SHAPE, shape).withProperty(PROPERTY_TYPE, type);
		}

		return state;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}
}
