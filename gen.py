"""generate full syntax from lox.java"""

import logging
import re
from typing import Dict, Tuple

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%m/%d/%Y %I:%M:%S %p",
)

logger = logging.getLogger(__name__)


def normal_replace(line) -> str:
    "replace common patterns."
    pattern_map = {
        "@main": "public static void main",
        "@sv": "static void",
        "is Double": "instanceof Double",
        "is @Bool": "instanceof Boolean",
        "is @str": "instanceof @str",
        "@str": "String",
        "@bool": "boolean",
        "@Bool": "Boolean",
        "eprintln!": "System.err.println",
        "println!": "System.out.println",
        "eprint!": "System.err.print",
        "print!": "System.out.print",
        " and ": " && ",
        " or ": " || ",
        " is ": " == ",
        "to_double": "Double.parseDouble",
    }
    for key, val in pattern_map.items():
        line = line.replace(key, val)
    return line


class Reader:
    "per-line reader"

    def __init__(self, lines):
        self.lines = lines
        self.index = 0

    def current(self) -> str:
        "current line"
        return normal_replace(self.lines[self.index])

    def advance(self) -> None:
        "to next"
        self.index += 1

    def is_end(self) -> bool:
        "running out lines"
        return self.index == len(self.lines)

    def get_params(self):
        "get full statement in following lines"
        line = self.current()
        statement = ""
        while ");" not in line and not self.is_end():
            statement += line
            self.advance()
            line = self.current()
        statement += line
        statement = statement[statement.find("(") + 1 : statement.find(");")]
        arr = get_tokens(statement)
        return arr[0], arr[1:]


def get_tokens(statement: str) -> list[str]:
    "split elements in statement"
    arr: list[str] = []
    index = 0

    def read_word() -> str:
        nonlocal statement, index
        tmp = ""
        while index < len(statement) and statement[index] != ",":
            tmp += statement[index]
            index += 1
        index += 1
        return tmp.strip()

    def read_str() -> str:
        nonlocal statement, index
        tmp = statement[index]
        index += 1
        while index < len(statement) and statement[index] != '"':
            tmp += statement[index]
            index += 1
        tmp += statement[index]
        index += 1

        index += 1
        return tmp.strip()[1:-1]

    while index < len(statement):
        char = statement[index]
        if char in " \n":
            index += 1
            continue
        if char == '"':
            arr.append(read_str())
        else:
            arr.append(read_word())
    return arr


def trans(key: str) -> str | None:
    "translate symbols"
    translation = {
        "(": "LEFT_PAREN",
        ")": "RIGHT_PAREN",
        "{": "LEFT_BRACE",
        "}": "RIGHT_BRACE",
        ",": "COMMA",
        ".": "DOT",
        "-": "MINUS",
        "+": "PLUS",
        ";": "SEMICOLON",
        "/": "SLASH",
        "*": "STAR",
        "!": "BANG",
        "!=": "BANG_EQUAL",
        "=": "EQUAL",
        "==": "EQUAL_EQUAL",
        ">": "GREATER",
        ">=": "GREATER_EQUAL",
        "<": "LESS",
        "<=": "LESS_EQUAL",
    }
    if key in translation:
        return translation[key]
    return None


def insert_tr(param_1: str, params: list[str]) -> str:
    "generate translation"
    params.insert(0, param_1)

    ret = ""
    for param in params:
        translated = trans(param)
        if translated is not None:
            ret += translated + ", "
        else:
            ret += param.upper() + ", "
    return ret + "\n"


def insert_tr_1(param_1, params) -> str:
    "generate tranlation, one parameter"
    param_1 = list(param_1)
    param_1, params = param_1[0], param_1[1:]
    result = insert_tr(param_1, params)
    return result


def case(param_1, params) -> str:
    "generate case statement"
    ret = ""
    for i in list(params[0]):
        ret += f"      case '{i}': {param_1}({trans(i)}); break;" + "\n"
    return ret


def gen_insert_cap(param_1, params) -> str:
    "generate map insert statements"
    ret = []
    for i in params:
        ret.append(f'    {param_1}.put("{i}", {i.upper()});')
    result = "\n".join(ret) + "\n"
    return result


def define_visitor(base_name: str, types: list[str]) -> str:
    "defining visitor"
    result = "  interface Visitor<R> {\n"

    for type_ in types:
        type_name = type_.split(":")[0].strip()
        result += (
            f"    R visit{type_name}{base_name}({type_name} {base_name.lower()});\n"
        )
    result += "  }\n"
    result += "\n"
    return result


g_classes = set()


def gen_ast(class_name: str, subclass_and_fields_list: list[str]):
    "generate class definations with visitor pattern"
    ret = f"abstract class {class_name} {{\n"
    ret += define_visitor(class_name, subclass_and_fields_list)

    for subclass_and_fields in subclass_and_fields_list:
        subclass_name = subclass_and_fields.split(":")[0].strip()
        fields = subclass_and_fields.split(":")[1].strip()
        ret += define_type(class_name, subclass_name, fields)
        g_classes.add(f"{class_name}.{subclass_name}")

    ret += "\n"
    ret += "  abstract <R> R accept(Visitor<R> visitor);\n"
    ret += "}\n"
    return ret


def gen_class_member(class_name: str, fields: list[str]) -> str:
    "generate class member and constructor"
    result = ""
    fields = fields[0].split(", ")
    map_ = {}
    for field in fields:
        type_, name = field.split()
        map_[type_] = name
        result += f"  {type_} {name};" + "\n"
    result += f"  {class_name}({', '.join(fields)}) {{" + "\n"
    for _, val in map_.items():
        result += f"    this.{val} = {val};" + "\n"
    result += "  }\n"
    return result


def define_type(parent_class, sub_class, fields):
    "defining subclass"
    # logger.info(f"input: {parent_class=} {sub_class=} {fields=}")
    result = f"  static class {sub_class} extends {parent_class} {{" + "\n"
    fields = fields.split(", ")
    for field in fields:
        type_, name = field.split()
        result += f"    {type_} {name};" + "\n"
    result += f"    {sub_class}({', '.join(fields)}) {{" + "\n"
    for field in fields:
        _, name = field.split()
        result += f"      this.{name} = {name};" + "\n"
    result += "    }\n"

    result += "\n"
    result += "    @Override\n"
    result += "    <R> R accept(Visitor<R> visitor) {\n"
    result += f"      return visitor.visit{sub_class}{parent_class}(this);\n"
    result += "    }\n"

    result += "  }\n"
    # logger.info(f"generated: {result}")
    return result


def gen_namespace(line: str) -> str:
    "generate package statement"
    ns_name = line.strip().split()[1]
    return f"package {ns_name};\n"


def gen_import(line: str) -> str:
    "generate import statements"
    pkgs = line.strip().split()[1].split(",")
    ret = ""
    for i in pkgs:
        ret += f"import java.{i}.*;\n"
    return ret


def transform(
    reader: Reader, state: list[str], pending_null: bool, brace_stack: str
) -> Tuple[bool, str]:
    "block transform"

    def match(line: str) -> bool:
        return reader.current().strip().startswith(line)

    if match("//"):
        state.append(reader.current())
        reader.advance()
        return pending_null, brace_stack

    header_map = {
        "@namespace": gen_namespace,
        "@import": gen_import,
    }

    for k, val in header_map.items():
        if match(k):
            state.append(val(reader.current()))
            reader.advance()
            return pending_null, brace_stack

    line = reader.current()

    if match("class"):
        g_classes.add(line.strip().split()[1].strip().replace("{", ""))

    if ":=" in line:
        arr = line.split(":=")
        assert len(arr) == 2
        var_name, rest = arr[0].strip(), arr[1].strip()
        line = transfer_head_space(arr[0], f"var {var_name} = {rest}\n")

    if match("@static"):
        line = reader.current()
        name = line.split()[-2]
        namespace = state[0].split()[-1][:-1]
        state.insert(2, f"import static {namespace}.{name}.*;\n")
        line = line.replace("@static ", "")
        state.append(line)
        reader.advance()
        return pending_null, brace_stack

    if match("@io_throw"):
        arr = line.rsplit("{")
        assert len(arr) == 2
        func_decl = arr[0].replace("@io_throw", "").strip()
        line = f"{func_decl} throws IOException {{\n"

    line = pipe_func(line, reader=reader)

    if "implements" in line:
        while "{" not in line:
            reader.advance()
            line += reader.current()
        parent_classes = line.split("implements")[1].replace("{", "").split(",")
        for i in parent_classes:
            arr = i.strip().split(".Visitor<")
            v_class = arr[0]
            v_type = arr[1][:-1]
            visitor_class_type_map[v_class] = v_type

    if match("@impl"):
        method_name = line.replace("@impl", "").replace("{", "").strip()
        if method_name.startswith("visit"):
            ret = re.findall(r"[A-Z][a-z]+", method_name)
            # logger.info(f"{ret=}")
            sub_class_name, parent_class_name = ret
            return_type = visitor_class_type_map[parent_class_name]
            line = transfer_head_space(
                line,
                f"@Override public {return_type} {method_name}"
                + f"({parent_class_name}.{sub_class_name} {parent_class_name.lower()}) {{\n",
            )
            if return_type == "Void":
                pending_null = True
    if pending_null:
        for char in line:
            if char == "{":
                brace_stack += char
            if char == "}":
                # breakpoint()
                brace_stack = brace_stack[:-1]
                if len(brace_stack) == 0:
                    pending_null = False
                    state.append("  " + transfer_head_space(line, "return null;\n"))

    state.append(line)
    reader.advance()
    return pending_null, brace_stack


def transfer_head_space(origin: str, output: str) -> str:
    "add origin's leading space to output"
    head_spaces = 0
    for _, val in enumerate(origin):
        if val == " ":
            head_spaces += 1
        else:
            break
    return " " * head_spaces + output


visitor_class_type_map: Dict[str, str] = {}


def pipe_func(line: str, reader: Reader) -> str:
    "apply macros"
    func_map = {
        "@insert_capval": gen_insert_cap,
        "@gen_ast": gen_ast,
        "@gen_class_member": gen_class_member,
        "@INSERT_TR_1": insert_tr_1,
        "@INSERT_TR": insert_tr,
        "@case": case,
    }

    def verify_cond(line):
        for macro, func in func_map.items():
            if line.strip().startswith(macro):
                return func
        return None

    func = verify_cond(line)
    if func is not None:
        param_1, params = reader.get_params()
        return func(param_1, params)

    return line


def get_lines(file_name: str) -> list[str]:
    "get lines from file"
    lines = []
    with open(file_name, encoding="utf-8", mode="r") as file:
        lines = file.readlines()
    return lines


def filter_line(line: str) -> str:
    "do last refinement"
    for i in g_classes:
        if f"{i}(" in line and "new" not in line and "{" not in line:
            line = line.replace(i, f"new {i}")
            if "=" in line and line.replace("static", "").strip().startswith("new"):
                line = line.replace("new ", "", 1)
            if ".new" in line:
                line = line.replace("new ", "", 1)
    if "return parse_error" in line:
        line = line.replace("return", "return new")
    if "== Expr." in line:
        line = line.replace("==", "instanceof")
    return line


def transform_all(in_file_name: str) -> list[str]:
    "read file and transform into lines"
    reader = Reader(get_lines(in_file_name))
    state: list[str] = []

    pending_null: bool = False
    brace_stack: str = ""
    while not reader.is_end():
        pending_null, brace_stack = transform(reader, state, pending_null, brace_stack)
    return state


def write_state(state: list[str], out_file_name: str) -> None:
    "do final replace and write"
    with open(out_file_name, mode="w", encoding="utf-8") as file:
        state = "\r\n".join(state).split("\r\n")
        for line in state:
            line = filter_line(line)
            file.write(line)


BNF = """
FILE -> NAMESPACE_DECL IMPORT_DECL CLASS_DECL+;
CLASS_DECL -> "class" $class_name "{"
    (GLOBAL_VAR|FUNC_DECL)+ "}";
GLOBAL_VAR -> ACCESS_MODIFIDER VAR_TYPE VAR_DECL;
VAR_TYPE -> $class_names | INTERAL_TYPE;
INTERAL_TYPE -> "String" | "Double" | COLLECTION_TYPE;
COLLECTION_TYPE -> ("List"|"ArrayList|"Map"|"HashMap") "<" VAR_TYPE ">";
ACCESS_MODIFIER -> VISIBILITY IS_STATIC;
VISIBILITY -> "public" | "private" | "protected" | "";
IS_STATIC -> "static" | "";
VAR_DECL -> VAR_NAME | (VAR_NAME '=' EXPR);
EXPR -> VALUE | UNARY | BINARY |GROUPING | "";
VALUE -> STRING| FUNC_CALL | NUMBER | "true" | "false"| "null";
UNARY -> ("-" | "!" ) EXPR;
BINARY ->EXPR OPERATOR EXPR:
GROUPING -> "(" EXPR ")";
OPERATOR -> "==" | "!=" | "<" | "<=" | ">" | ">=" | "+" | "-" | "*" | "/" ;

FUNC_CALL -> Object ("." FUNC_NAME)* "("
 ""| EXPR | EXPR ("," EXPR)*
 ")";
Object -> "new class_name" | VAR_NAME;
FUNC_DECL -> ACCESS_MODIFIDER TYPE FUNC_NAME "(" (VAR_TYPE VAR_NAME)* ")" THROW_STATEMENT "{" EXPRESSION* "}"

THROW_STATEMENT -> ""
        | "throws" EXCEPTION_TYPE ("," EZXCEPTION_TYPE)*;
EXCEPTION_TYPE -> class_name;
EXPRESSION -> IF_EXPR | FOR_EXPR |WHILE_EXPR | EXPR| RETURN;
RETURN -> "return" EXPR;
IF_EXPR -> "if" "(" EXPR ")" BLOCK
            ("else if" BLOCK)*
            ("else" BLOCK)*;
BLOCK -> EXPR | "{" EXPR+ "}";
FOR_EXPR -> "for" "(" EXPR; EXPR; EXPR ")" BLOCK_WITH_BREAK;
WHILE_EXPR -> "while" "(" EXPR ")" BLOCK_WITH_BREAK;
BLOCK_WITH_BREAK -> EXPR | "{" (EXPR| "break" )+ "}";

"""


class Expression:
    pass


class VarDecl:
    pass


class IfStmt:
    pass


class ForStmt:
    pass


class FuncDecl:
    pass


class ConstructorDecl:
    pass


class ClassDecl:
    def __init__(self, functions: list[FuncDecl],
    global_variables: list[VarDecl]) -> None:
        pass


class Tokenizer:
    "get tokens"

    def __init__(self, source) -> None:
        self.source = source


if __name__ == "__main__":
    g_classes.add("InputStreamReader")
    g_classes.add("BufferedReader")
    g_classes.add("StringBuilder")
    write_state(transform_all("lox.java"), "out.java")
