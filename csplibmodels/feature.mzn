int: N; % number of features
int: K; % number of releases
int: R; % number of resources
int: S; % number of stakeholders
%array[1..N] of string: feature_id;
array[1..N, 1..R] of int: r;
array[1..S, 1..N] of int: value;
array[1..S, 1..N, 1..3] of int: urgency;
array[1..3, 1..2] of int:C; % coupling
array[1..5, 1..2] of int:P; % precedence
array[1..S] of int: lambda; % stakeholder importance
array[1..K] of int: ksi; % release importance
% ksi is multiplied by 10 to have integers
array[1..K, 1..R] of int:Cap; %capacity
array[1..N, 1..K] of var int:
    WAS = array2d(1..N, 1..K,
        [ ksi[k]*sum (j in 1..S)
	  (lambda[j]*value[j,i]*urgency[j,i,k])
	    |i in 1..N, k in 1..K]);

% Variables  ==========
array[1..N]of var 1..K+1: x;% feature release number
var int: F; % objective function

% Constraints =========
constraint % dependency constraints
    forall ( i in index_set_1of2(C) )
      ( x[C[i,1]] = x[C[i,2]] )
    /\
    forall( i in index_set_1of2(P))
      (x[P[i,1]] <= x[P[i,2]] );

constraint % resource constraints
    forall( k in 1..K, j in 1..R)
      ( sum (i in 1..N)
      ( r[i,j]*bool2int( x[i] = k) ) <= Cap[k, j] );

constraint % objective function
    F = sum( k in 1..K, i in 1..N )
    ( WAS[i, k]*bool2int(x[i]=k));

solve maximize F;
output[ "x = "++show(x)++"\n"++"F = "++show(F)];