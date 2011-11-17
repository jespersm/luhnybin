#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <ctype.h>

/*
 * A simple stab at Square's Luhn filter challenge, this time in C
 * Written by Jesper Steen Møller aka @hr_Moller
 *
 *  Input:           56613959932537
 *  Expected result: XXXXXXXXXXXXXX
 */

#define MIN_DIGITS 14
#define MAX_DIGITS 16
#define BUFFER_SIZE (32*1024)

char input_buffer[BUFFER_SIZE];
char output_buffer[BUFFER_SIZE];

/* Luhn value for the even positions 0  1  2  3  4  5  6  7  8  9 */
int static LUHN_SUM_LOOKUP[] =     { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 };

// use normal or sum of double digits according to position
int luhn_digit(int digitPos, char cValue) {
	int value = cValue - '0';
	return digitPos % 2 == 0 ? LUHN_SUM_LOOKUP[value] : value;
}

size_t filter(char output[], char input[], size_t length) {
	int saw_digit = 0;
	int anchor = length - 1;
	size_t safe_anchor = 0;
	
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

int main(int argc, char *argv[])
{
	int length, written = 0;
	size_t start_pad = 0, safe_anchor = 0;
	
	while ((length = read(0,&input_buffer[start_pad],BUFFER_SIZE-start_pad)) != 0)
	{
		if (length == -1)
		{
			perror("Error reading from stdin");
			exit(1);
		}
		// copy what was just read into the output buffer, for masking
		memcpy(&output_buffer[start_pad], &input_buffer[start_pad], length);

		// valid input is now from 0 - length + start_pad
		safe_anchor = filter(output_buffer, input_buffer, length + start_pad);

		// now we've filtered all of the buffer, from 0 to length + start_pad
		// but the chars past the safe anchor may be filtered in the next run.
		// Go write what we can.
		if (safe_anchor > 0)
		{
			written = write(1,output_buffer,safe_anchor);
			if (written==-1)
			{
				perror("Error writing to stdout file");
				exit(1);
			}
		}
		// we just wrote out a bunch of chars, so now move to start of buffer
		start_pad = start_pad + length - safe_anchor;

		if (start_pad == BUFFER_SIZE) /* Sanity check */
		{
			fprintf(stderr, "Not enough buffer %d for crazy input!\n", BUFFER_SIZE);
			exit(1);
		}

		memmove(&input_buffer[0], &input_buffer[safe_anchor], start_pad);
		memmove(&output_buffer[0], &output_buffer[safe_anchor], start_pad);
		// now the first start_pad chars are ready for next pass
	}

	// There is no more input to read - write the remainder
	if (start_pad > 0)
	{
		written=write(1, output_buffer, start_pad);
		if (written==-1)
		{
			perror("Error writing remainder to stdout");
			exit(1);
		}
	}
}
