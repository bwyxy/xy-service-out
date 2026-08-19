interface ColumnType {
    key: string
    label: string
    prop: string
    width?: string | number
    type?: string,
    render?: ColumnRender,
    children?: ColumnType[]
}

interface ColumnRender {
    el: ('search' | 'select'),
    list?: Value[]
}

interface Value {
    label: string,
    value: string | number,
    elTagType: ( "primary" | "success" | "info" | "warning" | "danger")
}