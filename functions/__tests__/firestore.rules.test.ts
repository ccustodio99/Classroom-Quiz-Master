import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('Firestore security rules', () => {
  const projectId = 'quizmaster-test';
  const rulesPath = resolve(__dirname, '../../security/firestore.rules');
  const rules = readFileSync(rulesPath, 'utf8');
  let testEnv: ReturnType<typeof initializeTestEnvironment> extends Promise<infer T> ? T : never;

  beforeAll(async () => {
    testEnv = await initializeTestEnvironment({
      projectId,
      firestore: { rules },
    });
  });

  afterAll(async () => {
    await testEnv.cleanup();
  });

  afterEach(async () => {
    await testEnv.clearFirestore();
  });

  test('students cannot mutate score fields', async () => {
    await seedSession(testEnv);
    const student = testEnv.authenticatedContext('student1', {
      firebase: { sign_in_provider: 'anonymous' },
    });
    const participantRef = student.firestore().collection('sessions').doc('s1').collection('participants').doc('student1');
    await assertFails(participantRef.set({ totalPoints: 100 }));
    await assertSucceeds(participantRef.set({ nickname: 'Nova', avatar: 'spark' }));
  });

  test('teachers can only update their own sessions', async () => {
    await seedSession(testEnv);
    const otherTeacher = testEnv.authenticatedContext('teacher2', {
      firebase: { sign_in_provider: 'password' },
    });
    await assertFails(otherTeacher.firestore().collection('sessions').doc('s1').update({ status: 'active' }));

    const owner = testEnv.authenticatedContext('teacher1', {
      firebase: { sign_in_provider: 'password' },
    });
    await assertSucceeds(owner.firestore().collection('sessions').doc('s1').update({ status: 'active' }));
  });

  async function seedSession(env: typeof testEnv) {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await db.collection('sessions').doc('s1').set({
        teacherId: 'teacher1',
        status: 'lobby',
        currentIndex: 0,
        reveal: false,
        hideLeaderboard: false,
        lockAfterQ1: false,
        joinCode: 'ABC123',
        classroomId: 'c1',
        quizId: 'q1',
      });
    });
  }
});
