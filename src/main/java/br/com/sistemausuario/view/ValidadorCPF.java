package br.com.sistemausuario.view;

public class ValidadorCPF {

    public static boolean validar(String cpf) {
        cpf = cpf.replaceAll("[^\\d]", "");
        if (cpf.length() != 11) return false;
        if (cpf.matches("(\\d)\\1{10}")) return false;

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) soma += (cpf.charAt(i) - '0') * (10 - i);
            int resto = soma % 11;
            int primeiroDigito = (resto < 2) ? 0 : 11 - resto;

            soma = 0;
            for (int i = 0; i < 10; i++) soma += (cpf.charAt(i) - '0') * (11 - i);
            resto = soma % 11;
            int segundoDigito = (resto < 2) ? 0 : 11 - resto;

            return (primeiroDigito == (cpf.charAt(9) - '0')) &&
                    (segundoDigito == (cpf.charAt(10) - '0'));
        } catch (Exception e) {
            return false;
        }
    }

    public static String formatar(String cpf) {
        cpf = cpf.replaceAll("[^\\d]", "");
        if (cpf.length() == 11) {
            return cpf.substring(0,3) + "." +
                    cpf.substring(3,6) + "." +
                    cpf.substring(6,9) + "-" +
                    cpf.substring(9,11);
        }
        return cpf;
    }
}
