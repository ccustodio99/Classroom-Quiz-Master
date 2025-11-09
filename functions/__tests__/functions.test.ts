import * as admin from 'firebase-admin';
import firebaseFunctionsTest from 'firebase-functions-test';
import {
  exportReport,
  scoreAttempt,
  setBucketFactoryForTests,
  setFirestoreForTests,
  resetTestOverrides,
} from '../src/index';

describe('Cloud Functions', () => {
  const fft = firebaseFunctionsTest({ projectId: 'quizmaster-test' });

  afterEach(() => {
    resetTestOverrides();
    jest.restoreAllMocks();
  });

  afterAll(() => {
    fft.cleanup();
  });

  test('scoreAttempt computes points and updates submission', async () => {
    const attemptData = {
      uid: 'student1',
      questionId: 'q1',
      selected: ['A'],
      timeMs: 1_200,
    };
    const attemptRef = { id: 'attempt-1' } as admin.firestore.DocumentReference;
    const assignmentRef = { collection: jest.fn(() => ({ doc: () => submissionRef })) } as unknown as admin.firestore.DocumentReference;
    const quizRef = {} as admin.firestore.DocumentReference;
    const submissionRef = { id: 'student1' } as admin.firestore.DocumentReference;

    const assignmentData = {
      quizId: 'quiz1',
      attemptsAllowed: 3,
      scoringMode: 'best',
    };
    const quizData = {
      defaultTimePerQ: 30,
      questions: [
        { id: 'q1', answerKey: ['A'], timeLimitSeconds: 30 },
      ],
    };

    const transaction = {
      get: jest.fn(async (ref: unknown) => {
        if (ref === attemptRef) {
          return { exists: true, get: (field: string) => (attemptData as any)[field] };
        }
        if (ref === assignmentRef) {
          return { exists: true, data: () => assignmentData, get: (field: string) => (assignmentData as any)[field] };
        }
        if (ref === quizRef) {
          return { exists: true, data: () => quizData, get: (field: string) => (quizData as any)[field] };
        }
        if (ref === submissionRef) {
          return { exists: false, get: () => undefined };
        }
        throw new Error('Unexpected ref');
      }),
      update: jest.fn(),
      set: jest.fn(),
    };

    const firestoreStub = {
      collection: jest.fn((name: string) => {
        if (name === 'assignments') {
          return { doc: () => assignmentRef };
        }
        if (name === 'quizzes') {
          return { doc: () => quizRef };
        }
        throw new Error(`Unexpected collection ${name}`);
      }),
      runTransaction: jest.fn(async (fn: (txn: typeof transaction) => Promise<void>) => {
        await fn(transaction);
      }),
    } as unknown as admin.firestore.Firestore;

    setFirestoreForTests(firestoreStub);
    const snap = { data: () => attemptData, ref: attemptRef };
    const context = { params: { assignmentId: 'a1', attemptId: 'attempt-1' } } as any;

    const wrapped = fft.wrap(scoreAttempt);
    await wrapped(snap, context);

    expect(firestoreStub.runTransaction).toHaveBeenCalled();
    expect(transaction.update).toHaveBeenCalledWith(
      attemptRef,
      expect.objectContaining({ points: expect.any(Number), correct: true })
    );
    expect(transaction.set).toHaveBeenCalledWith(
      submissionRef,
      expect.objectContaining({ attempts: 1, bestScore: expect.any(Number) }),
      expect.objectContaining({ merge: true })
    );
  });

  test('exportReport validates ownership and returns signed URLs', async () => {
    const sessionData = { teacherId: 'teacher1' };
    const attempts = [
      {
        id: 'attempt-1',
        data: () => ({ uid: 'student1', questionId: 'q1', selected: ['A'], points: 900, timeMs: 1_200, correct: true }),
        get: (field: string) => ({
          uid: 'student1',
          questionId: 'q1',
          selected: ['A'],
          points: 900,
          timeMs: 1_200,
          correct: true,
        } as any)[field],
      },
    ];

    const attemptsCollection = {
      get: jest.fn(async () => ({ size: attempts.length, docs: attempts })),
    };

    const sessionRef = {
      get: jest.fn(async () => ({ exists: true, get: (field: string) => (sessionData as any)[field] })),
      collection: jest.fn((name: string) => {
        if (name === 'attempts') {
          return attemptsCollection;
        }
        throw new Error(`Unexpected subcollection ${name}`);
      }),
    } as unknown as admin.firestore.DocumentReference;

    const firestoreStub = {
      collection: jest.fn((name: string) => {
        if (name === 'sessions') {
          return { doc: () => sessionRef };
        }
        throw new Error(`Unexpected collection ${name}`);
      }),
      runTransaction: jest.fn(),
    } as unknown as admin.firestore.Firestore;

    const savedFiles: Record<string, { save: jest.Mock; getSignedUrl: jest.Mock }> = {};
    const bucket = {
      file: (path: string) => {
        const entry = savedFiles[path] ?? {
          save: jest.fn(async () => {}),
          getSignedUrl: jest.fn(async () => [`https://example.com/${path}`]),
        };
        savedFiles[path] = entry;
        return entry;
      },
    } as any;

    setFirestoreForTests(firestoreStub);
    setBucketFactoryForTests(() => bucket);

    const callable = fft.wrap(exportReport);
    const result = await callable(
      { sessionId: 's1' },
      { auth: { uid: 'teacher1', token: { firebase: { sign_in_provider: 'password' } } } }
    );

    expect(result.csvUrl).toContain('https://example.com/reports/s1/');
    expect(result.pdfUrl).toContain('https://example.com/reports/s1/');
    expect(result.accuracy).toBe(100);
    expect(Object.keys(savedFiles).length).toBe(2);
  });
});
