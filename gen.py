# pylint: disable=missing-module-docstring,missing-function-docstring,invalid-name,redefined-outer-name,missing-class-docstring

import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%m/%d/%Y %I:%M:%S %p",
)

logger = logging.getLogger(__name__)


def normal_replace(line) -> str:
    m = {
        "@psv": "public static void",
        "@sv": "static void",
        "@io_throw": "throws IOException",
        "@str": "String",
        "@bool": "boolean",
        "@out": "System.out",
        "@err": "System.err",
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


def trans(key: str) -> str:
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


def translate(p1: str, params: list[str]) -> str:
    params.insert(0, p1)

    ret = ""
    for p in params:
        translated = trans(p)
        if translated is not None:
            ret += translated + ", "
        else:
            ret += p.upper() + ", "
    return ret + "\n"


def translate_one(p1, params) -> str:
    p1 = list(p1)
    p1, params = p1[0], p1[1:]
    r = translate(p1, params)
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


def gen_ast(p1, params):
    ret = f"abstract class {p1} {{\n"
    for t in params:
        class_name = t.split(":")[0].strip()
        fields = t.split(":")[1].strip()
        ret += define_type(p1, class_name, fields)
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
    r = f"  static class {class_name} extends {p1} {{" + "\n"
    fields = fields.split(", ")
    map_ = {}
    for f in fields:
        type_, name = f.split()
        map_[type_] = name
        r += f"    {type_} {name};" + "\n"
    r += f"    {class_name}({', '.join(fields)}) {{" + "\n"
    for _, v in map_.items():
        r += f"      this.{v} = {v};" + "\n"
    r += "    }\n"
    r += "  }\n"
    return r


def transform(r: Reader) -> str:
    ret = ""
    if r.current().lstrip().startswith("//"):
        ret = r.current()
        r.advance()
        return ret
    l = normal_replace(r.current())
    func_map = {
        "@INSERT_CAP": gen_insert_cap,
        "@GEN_AST": gen_ast,
        "@gen_class_member": gen_class_member,
        "@translate_one": translate_one,
        "@translate": translate,
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
        ret = func(p1, params)
    else:
        ret = l
    r.advance()
    return ret


if __name__ == "__main__":
    lines = []
    with open("lox.java", encoding="utf-8", mode="r") as f:
        lines = f.readlines()
    r = Reader(lines)
    with open("out.java", mode="w", encoding="utf-8") as f:
        while not r.is_end():
            t = transform(r)
            f.write(t)
