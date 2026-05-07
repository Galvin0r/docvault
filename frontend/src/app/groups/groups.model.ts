export type Visibility = 'PRIVATE' | 'PUBLIC' | 'REQUEST_ONLY';

export const visibilityOptions = [
  { label: 'Public', value: 'PUBLIC' as Visibility },
  { label: 'Private', value: 'PRIVATE' as Visibility },
  { label: 'Request only', value: 'REQUEST_ONLY' as Visibility },
];

export type GroupRole = 'USER' | 'OWNER' | 'ADMIN';

export const rh = new Map<GroupRole, GroupRole>([
  ['USER', 'ADMIN'],
  ['ADMIN', 'OWNER'],
]);

export const getRoleByKey = (k: GroupRole) => rh.get(k);
export const getRoleByValue = (v: GroupRole) => {
  for (const [k, val] of rh) if (Object.is(val, v)) return k;
  return undefined;
};

export type GroupJoinRequestStatus = 'ACCEPTED' | 'PENDING' | 'REJECTED';

export interface Group {
  id: number;
  name: string;
  description: string;
  visibility: Visibility;
  created: string;
  membersNumber: number;
  requestsNumber: number;
}

export interface GroupMembership {
  id: number;
  userId: number;
  userLogin: string;
  groupId: number;
  groupName: string;
  role: GroupRole;
  created: string;
  groupVisibility: Visibility;
}

export interface GroupJoinRequest {
  id: number;
  userLogin: string;
  status: GroupJoinRequestStatus;
  created: string;
}