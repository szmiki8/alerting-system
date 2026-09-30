import { IsoDateTime } from './api-types';

/**
 * Admin resources (architecture Section 9.1, FR-23, FR-25, FR-26, FR-27).
 * Hand-written until the OpenAPI contract is published (OP-12, FE-27).
 */

/** Answer of `GET /api/v1/admin/me`. */
export interface CurrentAdmin {
  email: string;
  name: string;
}

/**
 * Subscriber type key, for example `email` or `slack`. Kept as a string because new subscriber
 * types can be added without UI changes to the list (NFR-14).
 */
export type SubscriberTypeKey = string;

/** Subscriber status (FR-21: Slack subscribers whose webhook is gone become INACTIVE). */
export type SubscriberStatus = 'ACTIVE' | 'INACTIVE';

/** One row of the subscriber list (FR-25). */
export interface SubscriberItem {
  /** Opaque subscriber ID, used for deletion. */
  id: string;
  type: SubscriberTypeKey;
  /** Name (email subscriber) or label (Slack subscriber); a Slack label is optional. */
  displayName: string | null;
  /** Email address, or the masked webhook identifier as delivered by the Core (NFR-04, OP-04). */
  address: string;
  subscribedAt: IsoDateTime;
  status: SubscriberStatus;
}

/** Query parameters of `GET /api/v1/admin/subscribers`. */
export interface SubscriberListQuery {
  /** Zero-based page index. */
  page: number;
  /** Page size; the Core caps it (100, OP-16). */
  size: number;
  /** Search text; left out when empty. */
  q?: string;
}

/** Answer of `GET /api/v1/admin/subscribers`: one page of subscribers with paging metadata. */
export interface SubscriberPage {
  items: SubscriberItem[];
  /** Zero-based index of this page. */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
