#include <stdio.h>
#include <stdbool.h>

float fatorial(float var);
int validar_entrada(float valor);

int main() {
	float numero;
	float resultado_fatorial;
	int entrada_valida;
	scanf("%f", &numero);
	printf("%f\n", (float)(numero));
	entrada_valida = validar_entrada(numero);
	if (entrada_valida) {
		resultado_fatorial = fatorial(numero);
		printf("%f\n", (float)(resultado_fatorial));
	}
	return 0;
}

float fatorial(float var) {
	float resultado;
	float contador;
	int valido;
	valido = 0;
	if ((var > 0.0)) {
		valido = 1;
	}
	if (valido) {
		resultado = 1.0;
		contador = 1.0;
		while ((contador < var)) {
			contador = (contador + 1.0);
			resultado = (resultado * contador);
		}
	}
	return resultado;
}

int validar_entrada(float valor) {
	int positivo;
	int menor_limite;
	positivo = 0;
	if ((valor > 0.0)) {
		positivo = 1;
	}
	menor_limite = 0;
	if ((valor < 20.0)) {
		menor_limite = 1;
	}
	return (positivo && menor_limite);
}

