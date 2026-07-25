export function arrayToUint8Array(array :any, littleEndian = false) {
    const view = new DataView(array)
    const length = array.byteLength;

    let deepCopy = new Uint8Array(length);

    for (let i = 0, j=0; i < length/2; i++) {
        const value = view.getInt16(i*2, true);
        const lowByte = value & 0xFF;
        const highByte = (value >> 8) & 0xFF;
        if (littleEndian) {
            deepCopy[j++] = lowByte;
            deepCopy[j++] = highByte;
        } else {
            deepCopy[j++] = highByte;
            deepCopy[j++] = lowByte;
        }
    }

    return deepCopy
}
