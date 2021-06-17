# AnalisadorSintático
Analisador Sintático criado para a disciplina MI - Processadores de Linguagem de Programação.

Este analisador dá continuação ao processo de compilação iniciado pelo Analisador Léxico desenvolvido anteriormente.

# AnalisadorSemântico
Analisador Semântico criado para a disciplina MI - Processadores de Linguagem de Programação.

## Estrutura de declaração de Struct

struct pessoa{
    var{
        string nome;
        real salario;
    }
}

var{
    struct pessoa Fernando;
}

## Uso do local e global

É permitido o uso de identificadores de mesmo nome, desde que em escopos diferentes. Caso um identificador
global tenha o mesmo nome que um identificador local, a linguagem permite distingui-los através das palavras global
e local, que devem ser colocados antes do nome da variável, juntamente com um delimitador . (ponto).

Quando o identificador não é precedido de "local." o analisador primeiramente procura se o mesmo foi declarado no escopo local, 
caso não encontre ele procura no escopo global.

# Estrutura Léxica da Linguagem 

| Descrição | Composição |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Palavras Reservadas  | `var` `const` `typeDef` `struct` `extends` `procedure` `function` `start` `return` `if` `else` `then` `while` `read` `print` `int` `real` `boolean` `string` `true` `false` `global` `local` |
| Identificadores (ID) | letra(letra \| digito \| `_` )\*|
| Números| Dígito+( . Dígito+)? |
| Dígito | [0-9] |
| Letra | [a-z] \| [A-Z] |
| Operadores Aritméticos | `+` `-` `*` `/` `++` `--` |
| Operadores Relacionais | `==` `!=` `>` `>=` `<` `<=` `=` |
| Operadores Lógicos | `&&` `\|\|` `!` |
| Delimitadores de Comentários | `//` Isto é um comentário de linha `/*` Isto é um comentário de bloco `*/`
| Delimitadores | `;` `,` `()` `[]` `{}` `.` |
| Cadeia de Caracteres (String )| "(letra \| digito \| simbolo \| `\"`)* " |
| Simbolo | ASCII de 32 a 126 (exceto ASCII 34) |
