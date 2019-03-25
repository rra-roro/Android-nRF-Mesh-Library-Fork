package no.nordicsemi.android.meshprovisioner.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Abstract class for bluetooth mesh addresses
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MeshAddress {

    private static final byte[] VTAD = "vtad".getBytes(Charset.forName("US-ASCII"));

    //Group address start and end defines the address range that can be used to create groups
    public static final int START_GROUP_ADDRESS = 0xC000;
    public static final int END_GROUP_ADDRESS = 0xFEFF;

    //Fixed group addresses
    public static final int ALL_PROXIES_ADDRESS = 0xFFFC;
    public static final int ALL_FRIENDS_ADDRESS = 0xFFFD;
    public static final int ALL_RELAYS_ADDRESS = 0xFFFE;
    public static final int ALL_NODES_ADDRESS = 0xFFFF;

    public static final int UNASSIGNED_ADDRESS = 0x0000;
    private static final int START_UNICAST_ADDRESS = 0x0001;
    private static final int END_UNICAST_ADDRESS = 0x7FFF;
    private static final byte B1_VIRTUAL_ADDRESS = (byte) 0x80;
    private static final int START_VIRTUAL_ADDRESS = 0x8000;
    private static final int END_VIRTUAL_ADDRESS = 0xBFFF;

    public static String formatAddress(final int address, final boolean add0x) {
        return add0x ? "0x" + String.format(Locale.US, "%04X", address) : String.format(Locale.US, "%04X", address);
    }

    public static boolean isAddressInRange(@NonNull final byte[] address) {
        return address.length != 2;
    }

    /**
     * Checks if the address is in range
     *
     * @param address address
     * @return true if is in range or false otherwise
     */
    public static boolean isAddressInRange(final int address) {
        return address == (address & 0xFFFF);
    }

    /**
     * Validates an unassigned address
     *
     * @param address 16-bit address
     * @return true if the address is a valid unassigned address or false otherwise
     */
    public static boolean isUnassignedAddress(@NonNull final byte[] address) {
        if (isAddressInRange(address)) {
            return false;
        }

        return isValidUnassignedAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Validates a unassigned address
     *
     * @param address 16-bit address
     * @return true if the address is a valid unassigned address or false otherwise
     */
    public static boolean isValidUnassignedAddress(final int address) {
        return isAddressInRange(address) && (address == UNASSIGNED_ADDRESS);
    }

    /**
     * Validates a unicast address
     *
     * @param address Address in bytes
     * @return true if the address is a valid unicast address or false otherwise
     */
    public static boolean isValidUnicastAddress(@NonNull final byte[] address) {
        if (isAddressInRange(address)) {
            return false;
        }

        return isValidUnicastAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Validates a unicast address
     *
     * @param address 16-bit address
     * @return true if the address is a valid unicast address or false otherwise
     */
    public static boolean isValidUnicastAddress(final int address) {
        return isAddressInRange(address) && (address >= START_UNICAST_ADDRESS && address <= END_UNICAST_ADDRESS);
    }

    /**
     * Validates a virtual address
     *
     * @param address Address in bytes
     * @return true if the address is a valid virtual address or false otherwise
     */
    public static boolean isValidVirtualAddress(@NonNull final byte[] address) {
        if (isAddressInRange(address)) {
            return false;
        }
        return isValidVirtualAddress(MeshParserUtils.unsignedBytesToInt(address[0], address[1]));
    }

    /**
     * Validates a unicast address
     *
     * @param address 16-bit address
     * @return true if the address is a valid virtual address or false otherwise
     */
    public static boolean isValidVirtualAddress(final int address) {
        if (isAddressInRange(address)) {
            return address >= START_VIRTUAL_ADDRESS && address <= END_VIRTUAL_ADDRESS;
        }
        return false;
    }


    private static boolean isValidGroupAddress(final byte[] address) {
        if (!isAddressInRange(address))
            return false;

        final int b0 = MeshParserUtils.unsignedByteToInt(address[0]);
        final int b1 = MeshParserUtils.unsignedByteToInt(address[1]);

        final boolean groupRange = b0 >= 0xC0 && b0 <= 0xFF;
        final boolean rfu = b0 == 0xFF && b1 >= 0x00 && b1 <= 0xFB;
        final boolean allNodes = b0 == 0xFF && b1 == 0xFF;

        return groupRange && !rfu && !allNodes;
    }

    /**
     * Validates a unicast address
     *
     * @param address 16-bit address
     * @return true if the address is valid and false otherwise
     */
    @SuppressWarnings({"ConstantConditions", "BooleanMethodIsAlwaysInverted"})
    public static boolean isValidGroupAddress(final int address) {
        if (!isAddressInRange(address))
            return false;

        final int b0 = address >> 8 & 0xFF;
        final int b1 = address & 0xFF;

        final boolean groupRange = b0 >= 0xC0 && b0 <= 0xFF;
        final boolean rfu = b0 == 0xFF && b1 >= 0x00 && b1 <= 0xFB;
        final boolean allNodes = b0 == 0xFF && b1 == 0xFF;

        return groupRange && !rfu && !allNodes;
    }

    /**
     * Returns the {@link AddressType}
     *
     * @param address 16-bit mesh address
     */
    @Nullable
    public static AddressType getAddressType(final int address) {
        if (isAddressInRange(address)) {
            if (isValidUnassignedAddress(address)) {
                return AddressType.UNASSIGNED_ADDRESS;
            } else if (isValidUnicastAddress(address)) {
                return AddressType.UNICAST_ADDRESS;
            } else if (isValidGroupAddress(address)) {
                return AddressType.GROUP_ADDRESS;
            } else {
                return AddressType.VIRTUAL_ADDRESS;
            }
        }
        return null;
    }

    /**
     * Generates a random uuid
     */
    public static UUID generateRandomLabelUUID() {
        return UUID.randomUUID();
    }


    /**
     * Returns the label UUID for a given virtual address
     *
     * @param address 16-bit virtual address
     */
    @Nullable
    public static UUID getLabelUuid(@NonNull final List<UUID> uuids, final int address) {
        if (MeshAddress.isValidVirtualAddress(address)) {
            for (UUID uuid : uuids) {
                final byte[] salt = SecureUtils.calculateSalt(VTAD);
                //Encrypt the label uuid with the salt as the key
                final byte[] encryptedUuid = SecureUtils.calculateCMAC(MeshParserUtils.uuidToBytes(uuid), salt);
                ByteBuffer buffer = ByteBuffer.wrap(encryptedUuid);
                buffer.position(12); //Move the position to 12
                final int hash = buffer.getInt() & 0x3FFF;
                if (hash == getHash(address)) {
                    return uuid;
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of the hash from a virtual address
     * <p>
     * The hash stored in a virtual address is derived from the label UUID.
     * In a virtual address bits 13 to 0 are set to the value of a hash of the corresponding Label UUID.
     * </p>
     *
     * @param address virtual address
     */
    public static int getHash(final int address) {
        if (isValidVirtualAddress(address)) {
            return address & 0x3FFF;
        }
        return 0;
    }
}
