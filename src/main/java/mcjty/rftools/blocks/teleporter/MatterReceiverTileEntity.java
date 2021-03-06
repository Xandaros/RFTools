package mcjty.rftools.blocks.teleporter;

import mcjty.lib.entity.GenericEnergyReceiverTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.varia.GlobalCoordinate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class MatterReceiverTileEntity extends GenericEnergyReceiverTileEntity implements ITickable {

    public static final String CMD_SETNAME = "setName";
    public static final String CMD_ADDPLAYER = "addPlayer";
    public static final String CMD_DELPLAYER = "delPlayer";
    public static final String CMD_SETPRIVATE = "setAccess";
    public static final String CMD_GETPLAYERS = "getPlayers";
    public static final String CLIENTCMD_GETPLAYERS = "getPlayers";

    private String name = null;
    private boolean privateAccess = false;
    private Set<String> allowedPlayers = new HashSet<String>();
    private int id = -1;

    private BlockPos cachedPos;

    public MatterReceiverTileEntity() {
        super(TeleportConfiguration.RECEIVER_MAXENERGY, TeleportConfiguration.RECEIVER_RECEIVEPERTICK);
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public int getOrCalculateID() {
        if (id == -1) {
            TeleportDestinations destinations = TeleportDestinations.getDestinations(worldObj);
            GlobalCoordinate gc = new GlobalCoordinate(getPos(), worldObj.provider.getDimension());
            id = destinations.getNewId(gc);

            destinations.save(worldObj);
            setId(id);
        }
        return id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        markDirtyClient();
    }

    public void setName(String name) {
        this.name = name;
        TeleportDestinations destinations = TeleportDestinations.getDestinations(worldObj);
        TeleportDestination destination = destinations.getDestination(getPos(), worldObj.provider.getDimension());
        if (destination != null) {
            destination.setName(name);
            destinations.save(worldObj);
        }

        markDirtyClient();
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            checkStateServer();
        }
    }

    private void checkStateServer() {
        if (!getPos().equals(cachedPos)) {
            TeleportDestinations destinations = TeleportDestinations.getDestinations(worldObj);

            destinations.removeDestination(cachedPos, worldObj.provider.getDimension());

            cachedPos = getPos();

            GlobalCoordinate gc = new GlobalCoordinate(getPos(), worldObj.provider.getDimension());

            if (id == -1) {
                id = destinations.getNewId(gc);
            } else {
                destinations.assignId(gc, id);
            }
            destinations.addDestination(gc);
            destinations.save(worldObj);

            markDirty();
        }
    }

    /**
     * This method is called after putting down a receiver that was earlier wrenched. We need to fix the data in
     * the destination.
     */
    public void updateDestination() {
        TeleportDestinations destinations = TeleportDestinations.getDestinations(worldObj);

        GlobalCoordinate gc = new GlobalCoordinate(getPos(), worldObj.provider.getDimension());
        TeleportDestination destination = destinations.getDestination(gc.getCoordinate(), gc.getDimension());
        if (destination != null) {
            destination.setName(name);

            if (id == -1) {
                id = destinations.getNewId(gc);
                markDirty();
            } else {
                destinations.assignId(gc, id);
            }

            destinations.save(worldObj);
        }
        markDirtyClient();
    }

    public boolean isPrivateAccess() {
        return privateAccess;
    }

    public void setPrivateAccess(boolean privateAccess) {
        this.privateAccess = privateAccess;
        markDirtyClient();
    }

    public boolean checkAccess(String player) {
        if (!privateAccess) {
            return true;
        }
        return allowedPlayers.contains(player);
    }

    public List<PlayerName> getAllowedPlayers() {
        List<PlayerName> p = new ArrayList<PlayerName>();
        for (String player : allowedPlayers) {
            p.add(new PlayerName(player));
        }
        return p;
    }

    public void addPlayer(String player) {
        if (!allowedPlayers.contains(player)) {
            allowedPlayers.add(player);
            markDirtyClient();
        }
    }

    public void delPlayer(String player) {
        if (allowedPlayers.contains(player)) {
            allowedPlayers.remove(player);
            markDirtyClient();
        }
    }

    public int checkStatus() {
        IBlockState state = worldObj.getBlockState(getPos().up());
        Block block = state.getBlock();
        if (!block.isAir(state, worldObj, getPos().up())) {
            return DialingDeviceTileEntity.DIAL_RECEIVER_BLOCKED_MASK;
        }
        block = worldObj.getBlockState(getPos().up(2)).getBlock();
        if (!block.isAir(state, worldObj, getPos().up(2))) {
            return DialingDeviceTileEntity.DIAL_RECEIVER_BLOCKED_MASK;
        }

        if (getEnergyStored(EnumFacing.DOWN) < TeleportConfiguration.rfPerTeleportReceiver) {
            return DialingDeviceTileEntity.DIAL_RECEIVER_POWER_LOW_MASK;
        }

        return DialingDeviceTileEntity.DIAL_OK;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        cachedPos = new BlockPos(tagCompound.getInteger("cachedX"), tagCompound.getInteger("cachedY"), tagCompound.getInteger("cachedZ"));
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        name = tagCompound.getString("tpName");

        privateAccess = tagCompound.getBoolean("private");

        allowedPlayers.clear();
        NBTTagList playerList = tagCompound.getTagList("players", Constants.NBT.TAG_STRING);
        if (playerList != null) {
            for (int i = 0 ; i < playerList.tagCount() ; i++) {
                String player = playerList.getStringTagAt(i);
                allowedPlayers.add(player);
            }
        }
        if (tagCompound.hasKey("destinationId")) {
            id = tagCompound.getInteger("destinationId");
        } else {
            id = -1;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        if (cachedPos != null) {
            tagCompound.setInteger("cachedX", cachedPos.getX());
            tagCompound.setInteger("cachedY", cachedPos.getY());
            tagCompound.setInteger("cachedZ", cachedPos.getZ());
        }
        return tagCompound;
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        if (name != null && !name.isEmpty()) {
            tagCompound.setString("tpName", name);
        }

        tagCompound.setBoolean("private", privateAccess);

        NBTTagList playerTagList = new NBTTagList();
        for (String player : allowedPlayers) {
            playerTagList.appendTag(new NBTTagString(player));
        }
        tagCompound.setTag("players", playerTagList);
        tagCompound.setInteger("destinationId", id);
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_SETNAME.equals(command)) {
            setName(args.get("name").getString());
            return true;
        } else if (CMD_SETPRIVATE.equals(command)) {
            setPrivateAccess(args.get("private").getBoolean());
            return true;
        } else if (CMD_ADDPLAYER.equals(command)) {
            addPlayer(args.get("player").getString());
            return true;
        } else if (CMD_DELPLAYER.equals(command)) {
            delPlayer(args.get("player").getString());
            return true;
        }
        return false;
    }

    @Override
    public List executeWithResultList(String command, Map<String, Argument> args) {
        List rc = super.executeWithResultList(command, args);
        if (rc != null) {
            return rc;
        }
        if (CMD_GETPLAYERS.equals(command)) {
            return getAllowedPlayers();
        }
        return null;
    }

    @Override
    public boolean execute(String command, List list) {
        boolean rc = super.execute(command, list);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_GETPLAYERS.equals(command)) {
            GuiMatterReceiver.storeAllowedPlayersForClient(list);
            return true;
        }
        return false;
    }
}
