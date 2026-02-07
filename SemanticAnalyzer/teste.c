#include <stdio.h>
#include <stdbool.h>

float magnitudeSUB2Valores(float var1, float var2);

int main() {
	float result;
	float valor1;
	float valor2;
	valor1 = 10.0;
	valor2 = 15.0;
	result = magnitudeSUB2Valores(valor1, valor2);
	printf("%f\n", (float)(result));
	return 0;
}

float magnitudeSUB2Valores(float var1, float var2) {
	float magnitude;
	int valid;
	valid = 0;
	if ((var1 > var2)) {
		valid = 1;
		magnitude = (var1 - var2);
		printf("%s\n", (valid) ? "true" : "false");
		return magnitude;
	}
	magnitude = (var2 - var1);
	printf("%s\n", (valid) ? "true" : "false");
	return magnitude;
}

