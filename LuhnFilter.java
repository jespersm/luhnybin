import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/*
 * A simple stab at Square's Luhn filter challenge.
 * Written by Jesper Steen Møller aka @hr_Moller
 */

public class LuhnFilter {

  private final int minDigits;
  private final int maxDigits;

  public LuhnFilter(int minDigits, int maxDigits) {
    this.minDigits = minDigits;
    this.maxDigits = maxDigits;
  }

  public char[] filter(char input[]) {
    // Clone the line so we can handle overlaps even if we 'X' digits out
    char output[] = input.clone();
    // iterate backwards
    for (int anchor = input.length-1; anchor >= minDigits-1; anchor--) {
      if (Character.isDigit(input[anchor])) {
        // We've something which may be a CC number
        int digitsConsidered = 1;
        int luhnSum = luhnDigit(digitsConsidered, input[anchor]); // ASCII digit -> int
        
        int mark = anchor;
        int startMark = anchor + 1;
        while (--mark >= 0 && digitsConsidered < maxDigits) {
          if (Character.isDigit(input[mark])) {
            luhnSum += luhnDigit(++digitsConsidered, input[mark]);
          } else if (input[mark] != ' ' && input[mark] != '-') {
            break; // consider next anchor
          }
          // is our CC number long enough and a valid Luhn number, then mark for X'ing
          if (digitsConsidered >= minDigits && (luhnSum % 10) == 0) startMark = mark; 
        }
        // done - now replace digits with X'es from startMark to anchor
        for (; startMark <= anchor; startMark++) { // NOP if valid CC was never found
          if (Character.isDigit(input[startMark])) output[startMark] = 'X';
        }
      }
    }
    return output;
  }

  // Luhn value for the even positions        // 0  1  2  3  4  5  6  7  8  9  
  private static final int[] LUHN_SUM_LOOKUP = { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 };

  // use normal or sum of double digits according to 
  private int luhnDigit(int digitPos, char cValue) {
    int value = cValue - '0';
    return digitPos % 2 == 0 ? LUHN_SUM_LOOKUP[value] : value;
  }

  // Plain main
  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(
        new InputStreamReader(System.in /*WARNING - uses default encoding*/));
    try {
      LuhnFilter lc = new LuhnFilter(14, 16);
      String line;
      while ((line = br.readLine()) != null) {
        char[] lineChars = line.toCharArray();
        System.out.println(lc.filter(lineChars));
      }
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        // Where's my JDK 7 try with resources ?
      }
    }
  }
}

