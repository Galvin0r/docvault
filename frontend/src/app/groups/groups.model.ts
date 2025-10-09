export type Visibility = 'PRIVATE' | 'PUBLIC' | 'REQUEST_ONLY';

export interface Group {
  id: number,
  name: string,
  description: string,
  visibility: Visibility,
  created: string,
  membersNumber: number
}