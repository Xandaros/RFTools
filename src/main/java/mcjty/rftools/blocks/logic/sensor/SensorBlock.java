package mcjty.rftools.blocks.logic.sensor;

import mcjty.lib.container.GenericGuiContainer;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.logic.generic.LogicSlabBlock;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class SensorBlock extends LogicSlabBlock<SensorTileEntity, SensorContainer> {

    public SensorBlock() {
        super(Material.IRON, "sensor_block", SensorTileEntity.class, SensorContainer.class);
    }

    @Override
    public boolean needsRedstoneCheck() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Class<? extends GenericGuiContainer> getGuiClass() {
        return GuiSensor.class;
    }

    private static long lastTime = 0;

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List<String> list, boolean whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            list.add(TextFormatting.WHITE + "This logic block gives a redstone signal");
            list.add(TextFormatting.WHITE + "depending on various circumstances in");
            list.add(TextFormatting.WHITE + "front of it. Like block placement, crop");
            list.add(TextFormatting.WHITE + "growth level, number of entities, ...");
        } else {
            list.add(TextFormatting.WHITE + RFTools.SHIFT_MESSAGE);
        }
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        TileEntity te = world.getTileEntity(data.getPos());
        if (te instanceof SensorTileEntity) {
            SensorTileEntity sensor = (SensorTileEntity) te;
            SensorType sensorType = sensor.getSensorType();
            if (sensorType.isSupportsNumber()) {
                probeInfo.text("Type: " + sensorType.getName() + " (" + sensor.getNumber() + ")");
            } else {
                probeInfo.text("Type: " + sensorType.getName());
            }
            int blockCount = sensor.getAreaType().getBlockCount();
            if (blockCount == 1) {
                probeInfo.text("Area: 1 block");
            } else {
                probeInfo.text("Area: " + blockCount + " blocks");
            }
            boolean rc = sensor.checkSensor();
            probeInfo.text(TextFormatting.GREEN + "Output: " + TextFormatting.WHITE + (rc ? "on" : "off"));
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        return currenttip;
    }


    @Override
    public int getGuiID() {
        return RFTools.GUI_SENSOR;
    }
}
