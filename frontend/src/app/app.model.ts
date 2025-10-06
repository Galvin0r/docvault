export type Page<T> = {
  content: T[],
  totalElements: number,
  size: number,
  number: number
}