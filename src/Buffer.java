package xy.lib;

import java.nio.*;

public class Buffer {

  public static final int Left = 1, Right = 2, Fill = 3;

  public Buffer(byte[] array) throws Exception {
    setBuffer(array, true);
  }

  public Buffer(byte[] array, boolean big) throws Exception {
    setBuffer(array, big);
  }

  public Buffer setBuffer(byte[] array) throws Exception {
    return setBuffer(array, true);
  }

  public Buffer setBuffer(byte[] array, boolean big) throws Exception {
    if ((buffer = array) == null) throw new Exception("Buffer: null array");
    index = 0;
    this.big = big;
    return this;
  }

  public Buffer setBigEndian(boolean big) {
    this.big = big;
    return this;
  }

  public boolean isBigEndian() {
    return big;
  }

  public Buffer seek(int pos) throws Exception {
    if (pos >= 0 && pos <= buffer.length) index = pos;
    else throw new Exception("Buffer.seek: bad index");
    return this;
  }

  public Buffer skip(int size) throws Exception {
    if ((size += index) >= 0 && size <= buffer.length) index = size;
    else throw new Exception("Buffer.skip: bad size");
    return this;
  }

  public int getIndex() {
    return index;
  }

  public int getByte(int pos) throws Exception {
    seek(pos);
    return getByte();
  }

  public int getByte() throws Exception {
    checkSpace(1);
    return buffer[index++];
  }

  public int getShort(int pos) throws Exception {
    seek(pos);
    return getShort();
  }

  public int getShort() throws Exception {
    checkSpace(2);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 2);
    int num = bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).getShort();
    index += 2;
    return num;
  }

  public int getInteger(int pos) throws Exception {
    seek(pos);
    return getInteger();
  }

  public int getInteger() throws Exception {
    checkSpace(4);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 4);
    int num = bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).getInt();
    index += 4;
    return num;
  }

  public long getLong(int pos) throws Exception {
    seek(pos);
    return getLong();
  }

  public long getLong() throws Exception {
    checkSpace(8);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 8);
    long num = bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).getLong();
    index += 8;
    return num;
  }

  public double getDouble(int pos) throws Exception {
    seek(pos);
    return getDouble();
  }

  public double getDouble() throws Exception {
    checkSpace(8);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 8);
    double num = bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).getDouble();
    index += 8;
    return num;
  }

  public String getString(int size, int pos) throws Exception {
    seek(pos);
    return getString(size);
  }

  public String getString(int size) throws Exception {
    checkSpace(size);
    int i = index, j = index += size;
    while (i < j && buffer[i] == ' ') i++;
    while (i < j && buffer[j - 1] == ' ') j--;
    return new String(buffer, i, j - i);
  }

  public int getNumber(int size, int pos) throws Exception {
    seek(pos);
    return getNumber(size);
  }

  public int getNumber(int size) throws Exception {
    String str = getString(size);
    if (str.charAt(0) == '+') str = str.substring(1);
    return Integer.parseInt(str);
  }

  public Buffer putByte(int num, int pos) throws Exception {
    seek(pos);
    return putByte(num);
  }

  public Buffer putByte(int num) throws Exception {
    checkSpace(1);
    buffer[index++] = (byte)num;
    return this;
  }

  public Buffer putShort(int num, int pos) throws Exception {
    seek(pos);
    return putShort(num);
  }

  public Buffer putShort(int num) throws Exception {
    checkSpace(2);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 2);
    bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).putShort((short)num);
    index += 2;
    return this;
  }

  public Buffer putInteger(int num, int pos) throws Exception {
    seek(pos);
    return putInteger(num);
  }

  public Buffer putInteger(int num) throws Exception {
    checkSpace(4);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 4);
    bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).putInt(num);
    index += 4;
    return this;
  }

  public Buffer putLong(long num, int pos) throws Exception {
    seek(pos);
    return putLong(num);
  }

  public Buffer putLong(long num) throws Exception {
    checkSpace(8);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 8);
    bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).putLong(num);
    index += 8;
    return this;
  }

  public Buffer putDouble(double num, int pos) throws Exception {
    seek(pos);
    return putDouble(num);
  }

  public Buffer putDouble(double num) throws Exception {
    checkSpace(8);
    ByteBuffer bb = ByteBuffer.wrap(buffer, index, 8);
    bb.order(big ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).putDouble(num);
    index += 8;
    return this;
  }

  public boolean putString(String str, int size) throws Exception {
    return storeString(str, size, Left);
  }

  public boolean putString(String str, int size, int pos) throws Exception {
    seek(pos);
    return storeString(str, size, Left);
  }

  public boolean putString(String str, int size, int pos, int align) throws Exception {
    seek(pos);
    return storeString(str, size, align);
  }

  public boolean putNumber(int num, int size) throws Exception {
    return storeString("" + num, size, Right);
  }

  public boolean putNumber(int num, int size, int pos) throws Exception {
    seek(pos);
    return storeString("" + num, size, Right);
  }

  public boolean putNumber(int num, int size, int pos, int align) throws Exception {
    seek(pos);
    String str = "" + num;
    if (align == Fill && (pos = str.length()) < size) {
      if (num < 0) str = str.substring(1);
      while (pos++ < size) str = "0" + str;
      if (num < 0) str = "-" + str;
    }
    return storeString(str, size, align);
  }

  public Buffer putData(byte[] data) throws Exception {
    return putData(data, 0, data.length);
  }

  public Buffer putData(byte[] data, int pos) throws Exception {
    seek(pos);
    return putData(data, 0, data.length);
  }

  public Buffer putData(byte[] data, int off, int len, int pos) throws Exception {
    seek(pos);
    return putData(data, off, len);
  }

  public Buffer putData(byte[] data, int off, int len) throws Exception {
    checkSpace(len);
    System.arraycopy(data, off, buffer, index, len);
    index += len;
    return this;
  }

  private void checkSpace(int size) throws Exception {
    if (size < 1) throw new Exception("Buffer.checkSpace: bad size");
    if (index + size > buffer.length) throw new Exception("Buffer.checkSpace: end of buffer");
  }

  private boolean storeString(String str, int size, int align) throws Exception {
    checkSpace(size);
    int len = (str = str.trim()).length();
    if (align == Right && len < size) fillSpace(size - len);
    if (len > 0) System.arraycopy(str.getBytes(), 0, buffer, index, len < size ? len : size);
    index += len < size ? len : size;
    if (align != Right && len < size) fillSpace(size - len);
    return len <= size;
  }

  private void fillSpace(int size) {
    while (size-- > 0) buffer[index++] = ' ';
  }

  private byte[] buffer;
  private int index;
  private boolean big;
}
