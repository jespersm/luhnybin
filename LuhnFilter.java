import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LuhnFilter {

  private final static int MIN_DIGITS=14;
  private final static int MAX_DIGITS=16;
  private final static int BUFFER_SIZE=32*1024;

  /*
   * A simple stab at Square's Luhn filter challenge, ported back to Java from C
   * Written by Jesper Steen Moeller aka @hr_Moller
   *
   *  Input:           56613959932537
   *  Expected result: XXXXXXXXXXXXXX
   */

  byte input_buffer[] = new byte[BUFFER_SIZE];
  byte output_buffer[] = new byte[BUFFER_SIZE];

  /* Luhn value for the even positions   0  1  2  3  4  5  6  7  8  9 */
  final static int LUHN_SUM_LOOKUP[] = { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 };

  // use normal or sum of double digits according to position
  int luhn_digit(int digitPos, byte cValue) {
    int value = cValue - '0';
    return digitPos % 2 == 0 ? LUHN_SUM_LOOKUP[value] : value;
  }

  int filter(byte output[], byte input[], int length) {
    int saw_digit = 0;
    int anchor = length - 1;
    int safe_anchor = 0;

    // iterate backwards
    for (; anchor >= 0; anchor--) {
      if (isdigit(input[anchor])) {
        // We've something which may be a CC number
        int digitsConsidered = 1;
        int luhn_sum = luhn_digit(digitsConsidered, input[anchor]); // ASCII digit -> int

        int mark = anchor;
        int startMark = anchor + 1;

        saw_digit = 1;
        while (--mark >= 0 && digitsConsidered < MAX_DIGITS) {
          if (isdigit(input[mark])) {
            luhn_sum += luhn_digit(++digitsConsidered, input[mark]);
          } else if (input[mark] != ' ' && input[mark] != '-') {
            if (safe_anchor == 0) safe_anchor = mark+1;
            break; // consider next anchor
          }
          // is our CC number long enough and a valid Luhn number, then mark for X'ing
          if (digitsConsidered >= MIN_DIGITS && (luhn_sum % 10) == 0) startMark = mark;
        }
        // done - now replace digits with X'es from startMark to anchor
        for (; startMark <= anchor; startMark++) { // NOP if valid CC was never found
          if (isdigit(input[startMark])) output[startMark] = 'X';
        }
      }
      else if (safe_anchor == 0 && input[anchor] != ' ' && input[anchor] != '-')
      {
        safe_anchor = anchor+1;
      }
    }
    return (saw_digit == 1) ? safe_anchor : length;
  }

  private boolean isdigit(byte b) {
    return '0' <= b && b <= '9';
  }

  void filter(InputStream inputStream, OutputStream outputStream) throws IOException
  {
    int length = 0;
    int start_pad = 0, safe_anchor = 0;

    while ((length = inputStream.read(input_buffer, start_pad, BUFFER_SIZE-start_pad)) != 0)
    {
      if (length == -1) break; // EOF
      // copy what was just read into the output buffer, for masking
      System.arraycopy(input_buffer, start_pad, output_buffer, start_pad, length);

      // valid input is now from 0 - length + start_pad
      safe_anchor = filter(output_buffer, input_buffer, length + start_pad);

      // now we've filtered all of the buffer, from 0 to length + start_pad
      // but the chars past the safe anchor may be filtered in the next run.
      // Go write what we can.
      if (safe_anchor > 0)
      {
        outputStream.write(output_buffer, 0, safe_anchor);
      }
      // we just wrote out a bunch of chars, so now move to start of buffer
      start_pad = start_pad + length - safe_anchor;

      if (start_pad == BUFFER_SIZE) /* Sanity check */
      {
        throw new RuntimeException("Not enough buffer " + BUFFER_SIZE + " for crazy input!\n");
      }

      System.arraycopy(input_buffer, safe_anchor, input_buffer, 0, start_pad);
      System.arraycopy(output_buffer, safe_anchor, output_buffer, 0, start_pad);
      // now the first start_pad chars are ready for next pass
    }

    // There is no more input to read - write the remainder
    if (start_pad > 0)
    {
      outputStream.write(output_buffer, 0, start_pad);
    }
  }

  public static void main(String[] args) throws IOException {
    new LuhnFilter().filter(System.in, System.out);
  } 
}
