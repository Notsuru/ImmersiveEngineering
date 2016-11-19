package blusunrize.immersiveengineering.common.blocks.cloth;

import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.shader.CapabilityShader;
import blusunrize.immersiveengineering.api.shader.CapabilityShader.ShaderWrapper_Direct;
import blusunrize.immersiveengineering.api.shader.IShaderItem;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ILightValue;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityConnectorStructural;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class TileEntityBalloon extends TileEntityConnectorStructural implements ILightValue, IPlayerInteraction, IHammerInteraction
{
	public int style = 0;
	public int colour0 = 0xffffff;
	public int colour1 = 0xffffff;
	public ShaderWrapper_Direct shader = new ShaderWrapper_Direct("immersiveengineering:balloon");

	@Override
	public int getLightValue()
	{
		return 13;
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt,descPacket);
		//to prevent old ballons from going black
		int nbtVersion = nbt.getInteger("nbtVersion");
		if(nbtVersion>=1)
		{
			style = nbt.getInteger("style");
			colour0 = nbt.getInteger("colour0");
			colour1 = nbt.getInteger("colour1");
		}
		if(nbt.hasKey("shader"))
		{
			shader = new ShaderWrapper_Direct("immersiveengineering:balloon");
			shader.deserializeNBT(nbt.getCompoundTag("shader"));
		}
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt,descPacket);
		nbt.setInteger("nbtVersion", 1);
		nbt.setInteger("style",style);
		nbt.setInteger("colour0",colour0);
		nbt.setInteger("colour1",colour1);
		nbt.setTag("shader", shader.serializeNBT());
	}

	@Override
	public float[] getBlockBounds()
	{
		return new float[]{.125f,0,.125f,.875f,.9375f,.875f};
	}

	@Override
	public boolean receiveClientEvent(int id, int arg)
	{
		if(id==0)
		{
			this.markContainingBlockForUpdate(null);
			return true;
		}
		return super.receiveClientEvent(id, arg);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
	{
		if(capability == CapabilityShader.SHADER_CAPABILITY)
			return true;
		return super.hasCapability(capability, facing);
	}
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
	{
		if(capability == CapabilityShader.SHADER_CAPABILITY)
			return (T)shader;
		return super.getCapability(capability, facing);
	}

	@Override
	public String getCacheKey(IBlockState object) {
		if(shader!=null && shader.getShaderItem()!=null && shader.getShaderItem().getItem() instanceof IShaderItem)
			return ((IShaderItem)shader.getShaderItem().getItem()).getShaderName(shader.getShaderItem());
		return colour0+":"+colour1+":"+style;
	}

	@Override
	public int getRenderColour(IBlockState object, String group)
	{
		if(shader!=null && shader.getShaderItem()!=null && shader.getShaderItem().getItem() instanceof IShaderItem)
			return 0xffffff;
		if (style==0)
		{
			if (group.startsWith("balloon1_"))
				return colour1;
			if (group.startsWith("balloon0_"))
				return colour0;
		}
		else
		{
			if (group.endsWith("_1"))
				return colour1;
			if (group.endsWith("_0"))
				return colour0;
		}
		return 0xffffff;
	}

	@Override
	public Vec3d getRaytraceOffset(IImmersiveConnectable link)
	{
		int xDif = ((TileEntity)link).getPos().getX()-getPos().getX();
		int zDif = ((TileEntity)link).getPos().getZ()-getPos().getZ();
		int yDif = ((TileEntity)link).getPos().getY()-getPos().getY();
		if(yDif<0)
		{
			double dist = Math.sqrt(xDif*xDif + zDif*zDif);
			if(dist/Math.abs(yDif)<2.5)
				return new Vec3d(.5,.09375,.5);
		}
		if(Math.abs(zDif)>Math.abs(xDif))
			return new Vec3d(.5,.09375,zDif>0?.8125:.1875);
		else
			return new Vec3d(xDif>0?.8125:.1875,.09375,.5);
	}
	@Override
	public Vec3d getConnectionOffset(Connection con)
	{
		int xDif = (con==null||con.start==null||con.end==null)?0: (con.start.equals(this.getPos())&&con.end!=null)? con.end.getX()-getPos().getX(): (con.end.equals(this.getPos())&& con.start!=null)?con.start.getX()-getPos().getX(): 0;
		int zDif = (con==null||con.start==null||con.end==null)?0: (con.start.equals(this.getPos())&&con.end!=null)? con.end.getZ()-getPos().getZ(): (con.end.equals(this.getPos())&& con.start!=null)?con.start.getZ()-getPos().getZ(): 0;
		int yDif = (con==null||con.start==null||con.end==null)?0: (con.start.equals(this.getPos())&&con.end!=null)? con.end.getY()-getPos().getY(): (con.end.equals(this.getPos())&& con.start!=null)?con.start.getY()-getPos().getY(): 0;
		if(yDif<0)
		{
			double dist = Math.sqrt(xDif*xDif + zDif*zDif);
			if(dist/Math.abs(yDif)<2.5)
				return new Vec3d(.5,.09375,.5);
		}
		if(Math.abs(zDif)>Math.abs(xDif))
			return new Vec3d(.5,.09375,zDif>0?.78125:.21875);
		else
			return new Vec3d(xDif>0?.78125:.21875,.09375,.5);
	}
	@Override
	public boolean interact(EnumFacing side, EntityPlayer player, EnumHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
	{
		if(heldItem!=null && heldItem.getItem() instanceof IShaderItem)
		{
			if(this.shader==null)
				this.shader = new ShaderWrapper_Direct("immersiveengineering:balloon");
			this.shader.setShaderItem(Utils.copyStackWithAmount(heldItem,1));
			markContainingBlockForUpdate(null);
			return true;
		}
		int target = 0;
		if(side.getAxis()==Axis.Y && style==0)
			target = (hitX<.375||hitX>.625)&&(hitZ<.375||hitZ>.625)?1:0;
		else if(side.getAxis()==Axis.Z)
		{
			if(style==0)
				target = (hitX<.375||hitX>.625)?1:0;
			else
				target =(hitY>.5625&&hitY<.75)?1:0;
		}
		else if(side.getAxis()==Axis.X)
		{
			if(style==0)
				target = (hitZ<.375||hitZ>.625)?1:0;
			else
				target =(hitY>.5625&&hitY<.75)?1:0;
		}
		int heldDye = Utils.getDye(heldItem);
		if(heldDye==-1)
			return false;
		heldDye = EnumDyeColor.byMetadata(15-heldDye).getMapColor().colorValue;
		if(target==0)
		{
			if(colour0==heldDye)
				return false;
			colour0 = heldDye;
		}
		else
		{
			if(colour1==heldDye)
				return false;
			colour1 = heldDye;
		}
		markContainingBlockForUpdate(null);
		return true;
	}
	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		style = 1-style;
		markContainingBlockForUpdate(null);
		return true;
	}
}