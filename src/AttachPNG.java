package xy.sys;

import java.io.*;
import java.util.zip.*;
import xy.lib.*;

class AttachPNG {

  public static void main(String[] args) throws Exception {
    if (args.length > 0) new AttachPNG(args);
    else System.out.println("usage$ AttachPNG input [message [output]]");
  }

  private AttachPNG(String[] args) throws Exception {
    read(args[0], args.length > 1);
    Inflater inf = new Inflater();
    inf.setInput(pack);
    int len = inf.inflate(image);
    System.out.println("Info: image inflate " + total + " -> " + len);
    restore();
    if (args.length > 2) {
      if (bits > 8) reduce();
      convert();
      attach(args[1]);
      filter();
      Deflater def = new Deflater();
      def.setInput(image, 0, (1 + line) * height);
      def.finish();
      total = def.deflate(pack);
      System.out.println("Info: image deflate " + (1 + line) * height + " -> " + total);
      write(args[2]);
    } else extract(args[1]);
  }

  private void read(String name, boolean save) throws Exception {
    InputStream fin = new FileInputStream(name);
    if (fin.read(array, 0, 8) < 8 || !match()) {
      System.out.println("Error: file " + name + " is not a PNG image.");
      System.exit(-1);
    } else while (fin.read(array, 0, 8) > 7) {
      int len = buffer.getInteger(0);
      String type = buffer.getString(4);
      if (type.equals("IHDR")) {
        fin.read(array, 8, len + 4);
        width = buffer.getInteger();
        height = buffer.getInteger();
        bits = buffer.getByte();
        line = size[color = buffer.getByte()] * bits / 8 * width;
        int comp = buffer.getByte();
        int filter = buffer.getByte();
        int inter = buffer.getByte();
        System.out.println("Info: image size " + width + "x" + height + ", bit depth " +
          bits + ", color type " + color + ", c/f/i " + comp + "/" + filter + "/" + inter);
        if (!save) System.exit(0);
        if (bits < 8) {
          System.out.println("Error: bit depth " + bits + " not supported.");
          System.exit(-1);
        } else pack = new byte [(1 + bits / 2 * width) * height];
        image = new byte [pack.length];
      } else if (type.equals("PLTE")) {
        fin.read(plte = new byte [len + 4], 0, len + 4);
      } else if (type.equals("IDAT")) {
        fin.read(pack, total, len + 4);
        total += len;
      } else fin.skip(len + 4);
    }
    fin.close();
  }

  private boolean match() {
    for (int i = 0; i < 8; i++) if (array[i] != signature[i]) return false;
    return true;
  }

  private void restore() {
    int pixel = size[color] * bits / 8, start = 0;
    for (int y = 0; y < height; y++, start += 1 + line) {
      if (image[start] == 0) continue;
      for (int x = 1; x <= line; x++) {
        int a = x > pixel ? image[start + x - pixel] & 0xff : 0;
        int b = y > 0 ? image[start + x - line - 1] & 0xff : 0;
        int c = x > pixel && y > 0 ? image[start + x - pixel - line - 1] & 0xff : 0;
        if (image[start] == 1) image[start + x] += a;
        else if (image[start] == 2) image[start + x] += b;
        else if (image[start] == 3) image[start + x] += a + b >> 1;
        else if (image[start] == 4) {
          int p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
          if (pa <= pb && pa <= pc) image[start + x] += a;
          else if (pb <= pc) image[start + x] += b;
          else image[start + x] += c;
        }
      }
    }
  }

  private void reduce() {
    line = size[color] * width;
    for (int y = 0; y < height; y++) for (int x = 0; x < line; x++)
      image[(1 + line) * y + 1 + x] = image[(1 + line * 2) * y + 1 + x * 2];
  }

  private void convert() {
    if (color == 3) {
      for (int y = 0, from = 0, to = 0; y < height; y++) {
        for (int x = 0; x < width; x++)
          System.arraycopy(plte, 3 * (image[from + 1 + x] & 0xff), pack, to + 1 + 3 * x, 3);
        from += 1 + width;
        to += 1 + 3 * width;
      }
      color = 2;
      plte = image;
      image = pack;
      pack = plte;
    } else if (color == 4) {
      for (int y = 0, from = 0, to = 0; y < height; y++) {
        for (int x = 0; x < width; x++) pack[to + 1 + x] = image[from + 1 + 2 * x];
        from += 1 + 2 * width;
        to += 1 + width;
      }
      color = 0;
      plte = image;
      image = pack;
      pack = plte;
    } else if (color == 6) {
      for (int y = 0, from = 0, to = 0; y < height; y++) {
        for (int x = 0; x < width; x++)
          System.arraycopy(image, from + 1 + 4 * x, pack, to + 1 + 3 * x, 3);
        from += 1 + 4 * width;
        to += 1 + 3 * width;
      }
      color = 2;
      plte = image;
      image = pack;
      pack = plte;
    }
    line = size[color] * width;
  }

  private void attach(String name) throws Exception {
    InputStream fin = new FileInputStream(name);
    int len = fin.read(pack, 0, total = line * height / 8);
    if (len + 8 > total) {
      System.out.println("Error: message too long, image too small.");
      System.exit(-1);
    } else fin.close();
    buffer.putInteger(magic, 0).putInteger(len);
    for (int i = 0; i < 8; i++) encode(array[i], 8 * i);
    for (int i = 0; i < len; i++) encode(pack[i], 64 + 8 * i);
  }

  private void filter() {
    for (int y = 0, start = 0; y < height; y++, start += 1 + line) {
      for (int x = line; x > size[color]; x--) image[start + x] -= image[start + x - size[color]];
      image[start] = 1;
    }
  }

  private void write(String name) throws Exception {
    if (new File(name).exists()) {
      System.out.print("Warning: file " + name + " exist, overwrite? (y/n) ");
      if (System.in.read() != 'y') System.exit(1);
    }
    OutputStream fout = new FileOutputStream(name);
    fout.write(signature);
    buffer.putInteger(13, 0).putString("IHDR", 4);
    buffer.putInteger(width).putInteger(height).putByte(8).putByte(color);
    for (int i = 0; i < 3; i++) buffer.putByte(0);
    CRC32 crc = new CRC32();
    crc.update(array, 4, 17);
    buffer.putInteger((int)crc.getValue());
    fout.write(array);
    buffer.putInteger(total, 0).putString("IDAT", 4);
    crc.reset();
    crc.update(array, 4, 4);
    crc.update(pack, 0, total);
    buffer.putInteger((int)crc.getValue());
    fout.write(array, 0, 8);
    fout.write(pack, 0, total);
    fout.write(array, 8, 4);
    buffer.putInteger(0, 0).putString("IEND", 4);
    crc.reset();
    crc.update(array, 4, 4);
    buffer.putInteger((int)crc.getValue());
    fout.write(array, 0, 12);
    fout.close();
  }

  private void extract(String name) throws Exception {
    for (int i = 0; i < 8; i++) array[i] = decode(8 * i);
    if (buffer.getInteger(0) != magic) {
      System.out.println("Error: no message attached in image.");
      System.exit(-1);
    } else total = buffer.getInteger();
    for (int i = 0; i < total; i++) pack[i] = decode(64 + 8 * i);
    if (new File(name).exists()) {
      System.out.print("Warning: file " + name + " exist, overwrite? (y/n) ");
      if (System.in.read() != 'y') System.exit(1);
    }
    OutputStream fout = new FileOutputStream(name);
    fout.write(pack, 0, total);
    fout.close();
  }

  private void encode(int number, int offset) {
    for (int i = 0, mask = 1; i < 8; i++, offset++, mask <<= 1)
      if ((number & mask) > 0) image[offset / line * (1 + line) + 1 + offset % line] |= 1;
      else image[offset / line * (1 + line) + 1 + offset % line] &= -2;
  }

  private byte decode(int offset) {
    byte number = 0;
    for (int i = 0, mask = 1; i < 8; i++, offset++, mask <<= 1)
      if ((image[offset / line * (1 + line) + 1 + offset % line] & 1) > 0) number |= mask;
    return number;
  }

  private static final byte[] signature = {-119, 80, 78, 71, 13, 10, 26, 10};
  private static final byte[] size = {1, 0, 3, 1, 2, 0, 4};
  private static final int magic = 0x57a9f55e;
  private byte[] array = new byte [25], pack, image, plte;
  private Buffer buffer = new Buffer(array);
  private int width, height, bits, color, line, total;
}
