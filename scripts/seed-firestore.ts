import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

initializeApp({
  credential: cert('serviceAccount.json')
});

const db = getFirestore();

async function seed() {
  console.log('Seeding sample org...');
  const orgRef = db.collection('orgs').doc('demo');
  await orgRef.set({ name: 'Demo Org' }, { merge: true });
}

seed().then(() => {
  console.log('Done');
  process.exit(0);
});
