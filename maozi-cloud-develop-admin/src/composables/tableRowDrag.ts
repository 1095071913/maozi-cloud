import type {Ref} from 'vue'

/**
 * el-table 行拖拽排序：事件委托到表格根节点（tbody 重渲染不丢监听），
 * 拖拽起点限定在 .drag-handle 上，避免影响单元格内的文本选择。
 * 行索引即表格数据顺序（使用方均未启用表格自身排序）。
 */
export function useTableRowDrag(
  tableRef: Ref<unknown>,
  onReorder: (fromIdx: number, toIdx: number, above: boolean) => void
): { bind: () => void } {
  let dragIdx = -1

  function root(): HTMLElement | undefined {
    return (tableRef.value as { $el?: HTMLElement } | null)?.$el
  }

  function rowIndexOf(el: HTMLElement, tr: HTMLElement): number {
    const tbody = el.querySelector('.el-table__body tbody')
    if (!tbody || !tbody.contains(tr)) return -1
    return Array.from(tbody.querySelectorAll('tr')).indexOf(tr as HTMLTableRowElement)
  }

  function clearMarks(el: HTMLElement): void {
    el.querySelectorAll('tr.row-dragging, tr.row-drop-above, tr.row-drop-below').forEach((tr) =>
      tr.classList.remove('row-dragging', 'row-drop-above', 'row-drop-below')
    )
  }

  function bind(): void {
    const el = root()
    if (!el) return

    el.addEventListener('dragstart', (e) => {
      const target = e.target as HTMLElement
      if (!target.closest('.drag-handle')) return
      const tr = target.closest('tr')
      const idx = tr ? rowIndexOf(el, tr) : -1
      if (idx < 0) return
      dragIdx = idx
      if (e.dataTransfer) {
        e.dataTransfer.setData('text/plain', String(idx))
        e.dataTransfer.effectAllowed = 'move'
      }
      clearMarks(el)
      tr!.classList.add('row-dragging')
    })

    el.addEventListener('dragover', (e) => {
      if (dragIdx < 0) return
      const tr = (e.target as HTMLElement).closest('tr')
      const idx = tr ? rowIndexOf(el, tr) : -1
      if (idx < 0 || idx === dragIdx) return
      e.preventDefault()
      if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
      clearMarks(el)
      const rect = tr!.getBoundingClientRect()
      tr!.classList.add(
        e.clientY < rect.top + rect.height / 2 ? 'row-drop-above' : 'row-drop-below'
      )
    })

    el.addEventListener('drop', (e) => {
      if (dragIdx < 0) return
      e.preventDefault()
      const tr = (e.target as HTMLElement).closest('tr')
      const idx = tr ? rowIndexOf(el, tr) : -1
      const fromIdx = dragIdx
      dragIdx = -1
      clearMarks(el)
      if (idx < 0 || idx === fromIdx) return
      const rect = tr!.getBoundingClientRect()
      onReorder(fromIdx, idx, e.clientY < rect.top + rect.height / 2)
    })

    el.addEventListener('dragend', () => {
      dragIdx = -1
      const current = root()
      if (current) clearMarks(current)
    })
  }

  return { bind }
}
