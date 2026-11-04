/**
 * 批量重命名项目：目录/文件名与文件内容的字符替换（类型定义）
 */

export interface RenameRule {
  /** 旧字符（非空） */
  from: string
  /** 新字符（可为空串 = 删除旧字符） */
  to: string
}

export interface RenameOptions {
  /** 是否替换文件内容（默认 true） */
  includeContent?: boolean
  /** 是否替换文件名称（默认 true） */
  renameFiles?: boolean
  /** 是否替换目录名称（含根目录自身，默认 true） */
  renameDirs?: boolean
  /** 仅处理文本类文件：扩展名白名单 + 内容 NUL 检测（默认 true，防止破坏二进制） */
  textOnly?: boolean
  /** 参与内容替换的单文件大小上限（MB，默认 16） */
  maxFileMB?: number
  /** 排除目录名（任意层级命中即整棵跳过） */
  excludeDirs?: string[]
}

export interface RenameStats {
  scannedFiles: number
  scannedDirs: number
  /** 内容发生替换的文件数 */
  contentChanged: number
  /** 内容替换总次数 */
  replaceCount: number
  filesRenamed: number
  dirsRenamed: number
  rootRenamed: boolean
  /** 超过大小上限被跳过的文件数 */
  skippedLarge: number
  /** 因目标已存在等原因被跳过的项目（最多记录 50 条） */
  errors: string[]
  cancelled: boolean
}

export type RenameStage = 'scan' | 'content' | 'rename' | 'done' | 'error'

export interface RenameProgress {
  stage: RenameStage
  /** 已完成项数 / 总项数（scan 阶段为已扫描条目数，无总量） */
  done: number
  total: number
  /** 当前处理路径（相对根目录） */
  current: string
  stats: RenameStats
}
