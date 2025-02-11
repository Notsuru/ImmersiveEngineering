/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.fluid.FluidUtils;
import blusunrize.immersiveengineering.client.utils.TextUtils;
import blusunrize.immersiveengineering.common.IETileTypes;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IComparatorOverride;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IEMultiblocks;
import blusunrize.immersiveengineering.common.util.CapabilityReference;
import blusunrize.immersiveengineering.common.util.DirectionUtils;
import blusunrize.immersiveengineering.common.util.LayeredComparatorOutput;
import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SheetmetalTankTileEntity extends MultiblockPartTileEntity<SheetmetalTankTileEntity>
		implements IBlockOverlayText, IPlayerInteraction, IComparatorOverride, IBlockBounds
{
	public FluidTank tank = new FluidTank(512*FluidAttributes.BUCKET_VOLUME);
	private final LayeredComparatorOutput comparatorHelper = new LayeredComparatorOutput(
			tank.getCapacity(),
			4,
			() -> world.notifyNeighborsOfStateChange(getPos(), getBlockState().getBlock()),
			layer -> {
				BlockPos masterPos = pos.subtract(offsetToMaster);
				for(int x = -1; x <= 1; x++)
					for(int z = -1; z <= 1; z++)
					{
						BlockPos pos = masterPos.add(x, layer+1, z);
						world.notifyNeighborsOfStateChange(pos, world.getBlockState(pos).getBlock());
					}
			}
	);
	private final List<CapabilityReference<IFluidHandler>> fluidNeighbors = new ArrayList<>();

	public SheetmetalTankTileEntity()
	{
		super(IEMultiblocks.SHEETMETAL_TANK, IETileTypes.SHEETMETAL_TANK.get(), true);
		// Tanks should not output by default
		this.redstoneControlInverted = true;
		for(Direction f : DirectionUtils.VALUES)
			if(f!=Direction.UP)
				fluidNeighbors.add(
						CapabilityReference.forNeighbor(this, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f)
				);
	}

	@Override
	public ITextComponent[] getOverlayText(PlayerEntity player, RayTraceResult mop, boolean hammer)
	{
		if(Utils.isFluidRelatedItemStack(player.getHeldItem(Hand.MAIN_HAND)))
		{
			SheetmetalTankTileEntity master = master();
			FluidStack fs = master!=null?master.tank.getFluid(): this.tank.getFluid();
			return new ITextComponent[]{TextUtils.formatFluidStack(fs)};
		}
		return null;
	}

	@Override
	public boolean useNixieFont(PlayerEntity player, RayTraceResult mop)
	{
		return false;
	}

	@Override
	public void tick()
	{
		checkForNeedlessTicking();
		if(isDummy()||world.isRemote)
			return;
		if(!isRSDisabled())
			for(CapabilityReference<IFluidHandler> outputRef : fluidNeighbors)
				if(tank.getFluidAmount() > 0)
				{
					int outSize = Math.min(144, tank.getFluidAmount());
					FluidStack out = Utils.copyFluidStackWithAmount(tank.getFluid(), outSize, false);
					IFluidHandler output = outputRef.getNullable();
					if(output!=null)
					{
						int accepted = output.fill(out, FluidAction.SIMULATE);
						if(accepted > 0)
						{
							int drained = output.fill(Utils.copyFluidStackWithAmount(out, Math.min(out.getAmount(), accepted), false), FluidAction.EXECUTE);
							this.tank.drain(drained, FluidAction.EXECUTE);
							this.markContainingBlockForUpdate(null);
						}
					}
				}
		comparatorHelper.update(tank.getFluidAmount());
	}

	@Override
	public Set<BlockPos> getRedstonePos()
	{
		return ImmutableSet.of(
				new BlockPos(1, 0, 1)
		);
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		tank.readFromNBT(nbt.getCompound("tank"));
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		CompoundNBT tankTag = tank.writeToNBT(new CompoundNBT());
		nbt.put("tank", tankTag);
	}

	@Override
	public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx)
	{
		if(posInMultiblock.getX()%2==0&&posInMultiblock.getY()==0&&posInMultiblock.getZ()%2==0)
			return VoxelShapes.create(.375f, 0, .375f, .625f, 1, .625f);
		return VoxelShapes.fullCube();
	}

	private static final BlockPos ioTopOffset = new BlockPos(1, 4, 1);
	private static final BlockPos ioBottomOffset = new BlockPos(1, 0, 1);
	private static final Set<BlockPos> ioOffsets = ImmutableSet.of(ioTopOffset, ioBottomOffset);

	@Override
	protected IFluidTank[] getAccessibleFluidTanks(Direction side)
	{
		SheetmetalTankTileEntity master = master();
		if(master!=null&&ioOffsets.contains(posInMultiblock))
			return new FluidTank[]{master.tank};
		return new FluidTank[0];
	}

	@Override
	protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource)
	{
		return ioOffsets.contains(posInMultiblock);
	}

	@Override
	protected boolean canDrainTankFrom(int iTank, Direction side)
	{
		return ioBottomOffset.equals(posInMultiblock);
	}

	@Override
	public boolean interact(Direction side, PlayerEntity player, Hand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
	{
		SheetmetalTankTileEntity master = this.master();
		if(master!=null)
		{
			if(FluidUtils.interactWithFluidHandler(player, hand, master.tank))
			{
				this.updateMasterBlock(null, true);
				return true;
			}
		}
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	private AxisAlignedBB renderAABB;

	@Override
	@OnlyIn(Dist.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if(renderAABB==null)
		{
			if(offsetToMaster.equals(BlockPos.ZERO))
				renderAABB = new AxisAlignedBB(getPos().add(-1, 0, -1), getPos().add(2, 5, 2));
			else
				renderAABB = new AxisAlignedBB(getPos(), getPos());
		}
		return renderAABB;
	}

	@Override
	public int getComparatorInputOverride()
	{
		if(ioBottomOffset.equals(posInMultiblock))
			return comparatorHelper.getCurrentMasterOutput();
		SheetmetalTankTileEntity master = master();
		if(offsetToMaster.getY() >= 1&&master!=null)//4 layers of storage
			return master.comparatorHelper.getLayerOutput(offsetToMaster.getY()-1);
		return 0;
	}
}