# pylint: disable=missing-module-docstring,missing-function-docstring,invalid-name,redefined-outer-name,missing-class-docstring,logging-fstring-interpolation

import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%m/%d/%Y %I:%M:%S %p",
)

logger = logging.getLogger(__name__)


def normal_replace(line) -> str:
    m = {
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
        "@Literal": "new Expr.Literal",
        "@Unary": "new Expr.Unary",
        "@Binary": "new Expr.Binary",
        "@Grouping": "new Expr.Grouping",
        "@T": "new token",
    }
    for k, v in m.items():
        line = line.replace(k, v)
    return line


class Reader:
    def __init__(self, lines):
        self.lines = lines
        self.index = 0

    def current(self) -> str:
        return self.lines[self.index]

    def advance(self) -> None:
        self.index += 1

    def is_end(self) -> bool:
        return self.index == len(self.lines)

    def get_params(self):
        line = self.current()
        s1 = ""
        while ");" not in line and not self.is_end():
            s1 += line
            self.advance()
            line = self.current()
        s1 += line
        s1 = s1[s1.find("(") + 1 : s1.find(");")]
        arr = get_tokens(s1)
        return arr[0], arr[1:]


def get_tokens(s: str) -> list[str]:
    arr: list[str] = []
    index = 0

    def read_word() -> str:
        nonlocal s, index
        tmp = ""
        while index < len(s) and s[index] != ",":
            tmp += s[index]
            index += 1
        index += 1
        return tmp.strip()

    def read_str() -> str:
        nonlocal s, index
        tmp = s[index]
        index += 1
        while index < len(s) and s[index] != '"':
            tmp += s[index]
            index += 1
        tmp += s[index]
        index += 1

        index += 1
        return tmp.strip()[1:-1]

    while index < len(s):
        c = s[index]
        if c in " \n":
            index += 1
            continue
        if c == '"':
            arr.append(read_str())
        else:
            arr.append(read_word())
    return arr


def trans(key: str) -> str | None:
    m = {
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
    if key in m:
        return m[key]
    return None


def insert_tr(p1: str, params: list[str]) -> str:
    params.insert(0, p1)

    ret = ""
    for p in params:
        translated = trans(p)
        if translated is not None:
            ret += translated + ", "
        else:
            ret += p.upper() + ", "
    return ret + "\n"


def insert_tr_1(p1, params) -> str:
    p1 = list(p1)
    p1, params = p1[0], p1[1:]
    r = insert_tr(p1, params)
    return r


def case(p1, params) -> str:
    ret = ""
    for i in list(params[0]):
        ret += f"      case '{i}': {p1}({trans(i)}); break;" + "\n"
    return ret


def gen_insert_cap(p1, params) -> str:
    ret = []
    for i in params:
        ret.append(f'    {p1}.put("{i}", {i.upper()});')
    r = "\n".join(ret) + "\n"
    return r


def define_visitor(base_name: str, types: list[str]) -> str:
    r = "  interface Visitor<R> {\n"

    for t in types:
        type_name = t.split(":")[0].strip()
        r += f"    R visit{type_name}{base_name}({type_name} {base_name.lower()});\n"
    r += "  }\n"
    r += "\n"
    return r


def gen_ast(p1, params):
    ret = f"abstract class {p1} {{\n"
    ret += define_visitor(p1, params)
    for t in params:
        class_name = t.split(":")[0].strip()
        fields = t.split(":")[1].strip()
        ret += define_type(p1, class_name, fields)
    ret += "\n"
    ret += "  abstract <R> R accept(Visitor<R> visitor);\n"
    ret += "}\n"
    return ret


def gen_class_member(class_name: str, fields: list[str]) -> str:
    r = ""
    fields = fields[0].split(", ")
    map_ = {}
    for f in fields:
        type_, name = f.split()
        map_[type_] = name
        r += f"  {type_} {name};" + "\n"
    r += f"  {class_name}({', '.join(fields)}) {{" + "\n"
    for _, v in map_.items():
        r += f"    this.{v} = {v};" + "\n"
    r += "  }\n"
    return r


def define_type(p1, class_name, fields):
    # logger.info(f"input: {p1=} {class_name=} {fields=}")
    r = f"  static class {class_name} extends {p1} {{" + "\n"
    fields = fields.split(", ")
    for f in fields:
        type_, name = f.split()
        r += f"    {type_} {name};" + "\n"
    r += f"    {class_name}({', '.join(fields)}) {{" + "\n"
    for f in fields:
        _, name = f.split()
        r += f"      this.{name} = {name};" + "\n"
    r += "    }\n"

    r += "\n"
    r += "    @Override\n"
    r += "    <R> R accept(Visitor<R> visitor) {\n"
    r += f"      return visitor.visit{class_name}{p1}(this);\n"
    r += "    }\n"

    r += "  }\n"
    # logger.info(f"generated: {r}")
    return r


def gen_namespace(s: str) -> str:
    ns_name = s.strip().split()[1]
    return f"package {ns_name};\n"


def gen_import(s: str) -> str:
    pkgs = s.strip().split()[1].split(",")
    ret = ""
    for i in pkgs:
        ret += f"import java.{i}.*;\n"
    return ret


def transform(r: Reader, state: list[str]) -> None:
    def match(x: str) -> bool:
        return r.current().strip().startswith(x)

    if match("//"):
        state.append(r.current())
        r.advance()
        return

    header_map = {
        "@namespace": gen_namespace,
        "@import": gen_import,
    }

    for k, v in header_map.items():
        if match(k):
            state.append(v(r.current()))
            r.advance()
            return

    l = normal_replace(r.current())

    if ":=" in l:
        arr = l.split(":=")
        assert len(arr) == 2
        var_name, rest = arr[0].strip(), arr[1].strip()
        l = transfer_head_space(arr[0], f"var {var_name} = {rest}\n")

    if match("@static"):
        line = r.current()
        name = line.split()[-2]
        ns = state[0].split()[-1][:-1]
        state.insert(2, f"import static {ns}.{name}.*;\n")
        line = line.replace("@static ", "")
        state.append(line)
        r.advance()
        return

    if match("@io_throw"):
        arr = l.rsplit("{")
        assert len(arr) == 2
        func_decl = arr[0].replace("@io_throw", "").strip()
        l = f"{func_decl} throws IOException {{\n"

    l = pipe_func(l)

    global visitor_class_type_map
    if "implements" in l:
        parent_classes = l.split("implements")[1].replace("{", "").split(",")
        for i in parent_classes:
            arr = i.strip().split(".Visitor<")
            v_class = arr[0]
            v_type = arr[1][:-1]
            visitor_class_type_map[v_class] = v_type

    global pending_null, brace_stack
    import re

    if match("@impl"):
        method_name = l.replace("@impl", "").replace("{", "").strip()
        ret = re.findall(r"[A-Z][a-z]+", method_name)
        # logger.info(f"{ret=}")
        sub_class_name, parent_class_name = ret
        return_type = visitor_class_type_map[parent_class_name]
        l = transfer_head_space(
            l,
            f"@Override public {return_type} {method_name}"
            + f"({parent_class_name}.{sub_class_name} {parent_class_name.lower()}) {{\n",
        )
        if return_type == "Void":
            pending_null = True
    if pending_null:
        for c in l:
            if c == "{":
                brace_stack += c
            if c == "}":
                # breakpoint()
                brace_stack = brace_stack[:-1]
                if len(brace_stack) == 0:
                    pending_null = False
                    state.append("  " + transfer_head_space(l, "return null;\n"))

    state.append(l)
    r.advance()


def transfer_head_space(origin: str, to: str) -> str:
    head_spaces = 0
    for i in range(len(origin)):
        if origin[i] == " ":
            head_spaces += 1
        else:
            break
    return " " * head_spaces + to


pending_null = False
brace_stack = ""

from typing import Dict

visitor_class_type_map: Dict[str, str] = {}


def pipe_func(l: str) -> str:
    func_map = {
        "@insert_capval": gen_insert_cap,
        "@gen_ast": gen_ast,
        "@gen_class_member": gen_class_member,
        "@INSERT_TR_1": insert_tr_1,
        "@INSERT_TR": insert_tr,
        "@case": case,
    }

    def verify_cond(l):
        for k, v in func_map.items():
            if l.strip().startswith(k):
                return v
        return None

    func = verify_cond(l)
    if func is not None:
        p1, params = r.get_params()
        return func(p1, params)

    return l


if __name__ == "__main__":
    lines = []
    with open("lox.java", encoding="utf-8", mode="r") as f:
        lines = f.readlines()
    r = Reader(lines)
    state: list[str] = []
    while not r.is_end():
        transform(r, state)
    with open("out.java", mode="w", encoding="utf-8") as f:
        for t in state:
            f.write(t)
