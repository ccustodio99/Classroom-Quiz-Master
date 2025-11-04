import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

admin.initializeApp();

export const incCounter = functions.firestore
  .document('orgs/{orgId}/counters/{counterId}/shards/{shardId}')
  .onWrite(async () => {
    // TODO: aggregate shard counts into parent document
    return null;
  });

export const onSubmissionWrite = functions.firestore
  .document('orgs/{orgId}/classes/{classId}/classwork/{workId}/submissions/{subId}')
  .onWrite(async (_change, _context) => {
    // TODO: push analytics events to BigQuery or Cloud Storage
    return null;
  });
