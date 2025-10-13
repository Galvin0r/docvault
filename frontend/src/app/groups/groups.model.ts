export type Visibility = 'PRIVATE' | 'PUBLIC' | 'REQUEST_ONLY';

export const visibilityOptions = [
  { label: 'Public', value: 'PUBLIC' as Visibility },
  { label: 'Private', value: 'PRIVATE' as Visibility },
  { label: 'Request only', value: 'REQUEST_ONLY' as Visibility },
];

export type GroupRole = 'USER' | 'OWNER' | 'ADMIN';

export interface Group {
  id: number;
  name: string;
  description: string;
  visibility: Visibility;
  created: string;
  membersNumber: number;
  allowedToAccess: boolean;
}

export interface GroupMembership {
  id: number;
  userId: number;
  userLogin: string;
  groupId: number;
  groupName: string;
  role: GroupRole;
}
